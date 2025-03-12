package com.example.demo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.controller.TestController;
import com.example.demo.controller.TestRController;
import com.example.demo.core.EndpointRegistrar;

@Configuration
public class Config {

    @Bean
    public TestController testController() {
        return new TestController();
    }

    @Bean
    public TestRController testRController() {
        return new TestRController();
    }

    @Bean
    @ConditionalOnExpression("!'reactive'.equalsIgnoreCase('${spring.main.web-application-type:}')")
    public EndpointRegistrar dynamicEndpointRegistrar() {
        return new EndpointRegistrar();
    }
}
