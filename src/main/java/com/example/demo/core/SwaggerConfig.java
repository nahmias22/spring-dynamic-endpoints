package com.example.demo.core;

import static org.springdoc.api.AbstractOpenApiResource.addRestControllers;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.ArrayList;
import java.util.List;

import org.springdoc.core.annotations.RouterOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${build.version:}")
    private String buildVersion;

    @Value("${build.timestamp:}")
    private String buildTimestamp;

    @Autowired
    private ApplicationContext applicationContext;

//    @Autowired
//    private RequestMappingHandlerMapping handlerMapping;

    @Bean
    public OpenAPI api() {
        OpenAPI openAPI = new OpenAPI()
                .addServersItem(new Server().url("/").description("Default Server URL"))
                .info(new Info()
                        .title("DEMO App")
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

//    @Bean
//    public RouterFunction<ServerResponse> dynamicRoutes() {
//        RouterFunction<ServerResponse> routes = (RouterFunction<ServerResponse>) applicationContext.getBean("route");
//        return routes;
//    }

}
