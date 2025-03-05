package com.example.demo.core;

import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;

import com.example.demo.util.Json;

import reactor.core.publisher.Mono;

@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")
public class ReactiveEndpointRegistrar {

    Logger log = LoggerFactory.getLogger(ReactiveEndpointRegistrar.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public RouterFunction<ServerResponse> route() {
        RequestMappingHandlerMapping handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        RouterFunctions.Builder builder = RouterFunctions.route();

        builder.GET("/get",  request-> ServerResponse.ok().bodyValue("dummy"));

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
                    log.info("Registering path: " + Json.toString(methodMapping.method()) + " - " + path + " for method: " + method.getName());

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
                }
            }
        }

        var requestMappingHandlers = ((RequestMappingHandlerMapping)applicationContext.getBean("requestMappingHandlerMapping")).getHandlerMethods()
                .keySet();
        log.info(Json.toString(requestMappingHandlers));

        return builder.build();
    }

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

    private String getPath(RequestMapping classRM, RequestMapping methodRM) {
        if (methodRM.path().length == 0)
            return classRM.path()[0];

        return  classRM.path()[0] + methodRM.path()[0];
    }
}
