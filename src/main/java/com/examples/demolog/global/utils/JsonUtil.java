package com.examples.demolog.global.utils;


import com.examples.demolog.global.exception.CommonErrorCode;
import com.examples.demolog.global.exception.UtilsException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtil {

    private static ObjectMapper objectMapper;

    public static void init(ObjectMapper objectMapper) {
        JsonUtil.objectMapper = objectMapper;
    }

    public static <T> String toJsonStr(T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new UtilsException(CommonErrorCode.JSON_PROCESSING_ERROR);
        }
    }

    public static <T> T fromJsonStr(String jsonString, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (JsonProcessingException e) {
            throw new UtilsException(CommonErrorCode.JSON_PROCESSING_ERROR);
        }
    }

}