# Spring-Dynamic-Endpoints
## Introduction
This project will provide 2 examples on how to programmatically expose rest endpoints, in either a Spring Web or Spring WebFlux application, without the traditional use of spring's ``@RestController`` or ``@Controller`` annotations.

The application created here can be run both as a spring-web and a spring-webflux application by either setting in the application properties the following:

``spring.main.web-application-type=reactive``

If not defined (commented out), the application will operate as a Spring Web application.

## The Problem
As software engineers or developers creating web applications we usually have 2 main goals:
1. Create code that works
2. provide proper documentation of the exposed endpoints

I have always had a pet peeve with spring and exposing apis and how the most recommended solution was to use either the ``@RestController`` or ``@Controller`` annotations. In regard to the documentation I will be discussing the dependencies provided by spring-doc and swagger v3. In order for this library to recognize your endpoint classes to document it will also look for the classes annotated by those same annotations. Thus, there is an overall dependency on those annotations, which, in my opinion, brings a few limitations.

In general, we can make configuration classes and instantiate beans of classes that we want active when the application is running, that can be autowired in where needed. For example, let's hypothesise that we have an application with a manager A that performs a specific task. Most of our clients are happy with the results provided by Manager A, but for some clients, it may need to do something extra or different. In this case we can extend manager A creating manager B, override the code that needs to be altered, and finally, in the configuration class, return a bean of Manager B instead of A upon appropriate conditions.

But what about controller classes? Similar to the previous scenario, what if we had a controller class with a specific endpoint signature acceptable to most clients. But for some an extra header might be needed an extra query param or header. We could extend controller A, creating controller B overriding only the required method, but then? If we annotated both controller with the @RestController when running the application we would get exceptions, of trying to expose 2 methods to the same path. 

Or in an even simpler case some clients may not want some endpoints exposed for their respective reasons. What then? We could use the @Conditional annotation on the different controller classes according to our needs, but that would spread the configuration across the application.

This brings us to the problem's requirements:
1. Implement a way to manage the exposed controllers in our configuration classes
2. Document the endpoints according to what has been exposed based on the aforementioned configuration 

## The Proposed Solution

### The big picture
The solution proposed in this article is the following:
1. First start instantiating Beans for the controller classes that need to be exposed
2. Create a class that will:
   1. Identify any beans in the application context that are controller classes.
   2. From those classes, register any methods annotated with a mapping annotation as mappings in the application's request mapping handler.
3. Configure the documentation library to process only the classes that have been instantiated as beans and exposed.

### Software implementation
The first thing that needs to be done is to stop using the spring annotations, which raises the question of how will the relevant endpoints be exposed?

This is where the class ``EndpointRegistrar`` comes in. Having first created beans of all the controller classes that should be active (thus gaining control over what is actually exposed), we then filter the application context to find any classes that are annotated with the ``@RequestMapping`` annotation. Hence, we are able to retrieve all the classes that contain endpoints that need to be exposed. From that point on it is just a matter to getting from the context the ``RequestMappingHandlerMapping`` and register all the mappings for each of the relevantly annotated methods found in those classes.

This is the boiled down version of the approach but as this is application that runs either based on the ``web.servlet dependencies`` or the ``web.reactive``, each have their own implementation of the ``RequestMappingHandlerMapping``, accordingly the ``ReactiveEndpointRegistrar`` 

I want to point out, before continuing on that for each of the endpoints inside the controller class, that instead of using the annotations ``@GetMapping``, ``@PostMapping``, ``@PutMapping`` and ``@DeleteMapping``, the ``@RequestMapping`` annotation is still used and the method is defined within it. This is done to make the code in the registrar more simple:
```java
for (Method method : methods) {
    RequestMapping requestMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
    if (requestMapping != null) {
        String[] path = getPath(classRequestMapping, requestMapping);
        RequestMappingInfo requestMappingInfo = RequestMappingInfo.paths(path)
                .methods(requestMapping.method())
                .produces(requestMapping.produces())
                .build();
        handlerMapping.registerMapping(requestMappingInfo, bean, method);
        log.info("Exposing endpoint: {} - {}", requestMapping.method(), path[0]);
    }
}
```
When checking each of the methods found in the class whose endpoints need to be exposed we only need to check that the method is annotated by the same annotation and not any of the other 4.

This is the point were the solution for each operating mode diverged from each other. While this was sufficient for the web implementation, when having just this in the reactive registrar the endpoint would be exposed but when a request was sent the application did not know what method it had to use in order to serve it. Hence, it is also needed to define functionally the method that that should be triggered when a request is received.

```java
@Bean
    public RouterFunction<ServerResponse> route() {
        RequestMappingHandlerMapping handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        RouterFunctions.Builder builder = RouterFunctions.route();

        // Iterate over beans with @RequestMapping annotations
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RequestMapping.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            RequestMapping classMapping = AnnotationUtils.findAnnotation(beanClass, RequestMapping.class);
            // Find @RequestMapping annotations on methods
            Method[] methods = beanClass.getDeclaredMethods();
            for (Method method : methods) {
                RequestMapping methodMapping = AnnotationUtils.findAnnotation(method, RequestMapping.class);
                if (methodMapping != null) {
                    String path = getPath(classMapping, methodMapping);

                    RequestMethod httpMethod = Arrays.stream(methodMapping.method()).findFirst().get();
                    switch (httpMethod) {
                        case POST -> builder.POST(path, request -> invokeMethod(bean, method, request));
                        case PUT -> builder.PUT(path, request -> invokeMethod(bean, method, request));
                        case GET -> builder.GET(path, request -> invokeMethod(bean, method, request));
                        case DELETE ->builder.DELETE(path, request -> invokeMethod(bean, method, request));
                    }
                    RequestMappingInfo requestMappingInfo = RequestMappingInfo.paths(path)
                            .mappingName(method.getName())
                            .methods(methodMapping.method())
                            .produces(methodMapping.produces())
                            .build();

                    handlerMapping.registerMapping(requestMappingInfo, bean, method);
                    log.info("Exposing endpoint: [{}] - {}", httpMethod, path);
                }
            }
        }
        return builder.build();
    }
```
The first difference that can be seen in the reactive registrar is the fact that the logic is not implemented in a post construct method but a bean defining routes in the application. Similarly to the other registrar we retrieve the relevant classes from the application context and for each class, its methods are analysed and processed accordingly. While we are able to still utilise the traditional or usual way for defining endpoints the return value needs to be encapsulated in a ``ServerResponse`` which is where the ``invokeMethod`` function comes in:

```java
private Mono<ServerResponse> invokeMethod(Object bean, Method method, ServerRequest request) {
        try {
            // Invoke the method on the bean
            Object result = method.invoke(bean);
            if (result instanceof Mono<?> monoResult) {
                // Return Mono directly if the method returns a reactive type
                return monoResult.flatMap(response -> ServerResponse.ok().bodyValue(response));
            } else {
                // Wrap non-reactive result in Mono
                return ServerResponse.ok().bodyValue(result);
            }
        } catch (Exception e) {
            return ServerResponse.status(500).bodyValue("Error: " + e.getCause().getMessage());
        }
    }
```
One could argue that doing both, defining the route and registering the mapping to the ``RequestMappingHandlerMapping`` is redundant, but they would be wrong. At least with this implementation, and this will be made clear in the following section.

### Documentation
In today's era of development where developers need to integrate their applications with other applications, documentation is key and there are a number of libraries to assist. Here we are using spring-doc's open-api and again both the webmvc-ui and webflux-ui, for each mode of operation of the application.

So since we decided not to use the ``@RestController`` or ``@Controller`` annotations when starting the application the generated swagger and the ui would show no methods as the libraries would not have known what classes to document. But here spring-doc offers us a solution of being able to manually add controllers that should be documented:
```java
@Bean
    public OpenAPI api() {
        OpenAPI openAPI = new OpenAPI()
                .addServersItem(new Server().url("/").description("Default Server URL"))
                .info(new Info()
                        .title(applicationName)
                        .description("Demo Application")
                        .contact(new Contact()
                                .name("Philip G. Nahmias")
                                .email("nahmias22@gmail.com")));


        List<Class> controllers = new ArrayList<>();
        applicationContext.getBeansWithAnnotation(RequestMapping.class).forEach((k, v) -> {
            if (!k.endsWith("ErrorController")) {
                controllers.add(v.getClass());
            }
        });
        addRestControllers(controllers.toArray(new Class[0]));
        return openAPI;
    }
```
And this will is the reason why in the case of the reactive registrar we had to utilise both registration methods. While it would have been sufficient to only register the routes programmatically and any requests would be served, spring-doc would not document the exposed end-points. That is because in the ``OpenApiResource`` class of the ``org.springdoc.webflux.api`` package contains its own implementation of the method ``calculatePath``. And when it starts processing the additional rest controllers that we added manually, it will go and check if there is an exposed path that corresponds to an endpoint from these controllers. Thus, we also need to make sure that the paths that are being exposed are registered with the relevant ``RequestMappingHandlerMapping``.

## Conclusion
In this article I had no intention to delve into the debate over the use of annotations in Spring and their merits and drawbacks. As I mention in the introduction the framework offers a number of automations that we often take for granted and don't need to examine more closely. So if one can structure their application code in such a way that annotations fulfill all their needs, I say good for them!

The reality is that for both modes of operation, a significant layer of complexity is added to the application, but in exchange we do gain a significant level of control over the exposed endpoints.