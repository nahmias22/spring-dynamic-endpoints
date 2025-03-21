package com.example.demo.core;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.example.demo.util.Json;

import jakarta.annotation.PostConstruct;

public class EndpointRegistrar {

    Logger log = LoggerFactory.getLogger(EndpointRegistrar.class);

    @Autowired
    private ApplicationContext applicationContext;

    private String[] getPath(RequestMapping classRM, RequestMapping methodRM) {
        if (methodRM.path().length == 0)
            return classRM.path();

        String[] result = new String[1];
        result[0] = classRM.path()[0] + methodRM.path()[0];
        return result;
    }

    @PostConstruct
//    @ConditionalOnExpression("!'reactive'.equalsIgnoreCase('${spring.main.web-application-type:}')")
    public void registerControllers() {
        RequestMappingHandlerMapping handlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        applicationContext.getBeansWithAnnotation(RequestMapping.class).forEach((name, bean) -> {
            if (!name.endsWith("ErrorController")) {
                Class<?> controllerClass = bean.getClass();
                RequestMapping classRequestMapping = AnnotationUtils.findAnnotation(controllerClass, RequestMapping.class);
                Method[] methods = controllerClass.getDeclaredMethods();
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
            }
        });
    }
}
