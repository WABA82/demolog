package com.examples.demolog.global.config;

import com.examples.demolog.global.utils.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonUtilConfig {

    private final ObjectMapper objectMapper;

    public JsonUtilConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        JsonUtil.init(objectMapper);
    }

}
