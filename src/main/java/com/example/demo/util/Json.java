package com.example.demo.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.annotation.PostConstruct;

@Service
public class Json {

    static ObjectMapper objectMapper = new ObjectMapper();

    public <T> T convertToObject(String value, Type type) {
        try {
            return objectMapper.readValue(value, objectMapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}