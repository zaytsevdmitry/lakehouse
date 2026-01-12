package org.lakehouse.client.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ObjectMapping {
    private static final ObjectMapper objectMapper = new ObjectMapperTS();

    public static <T> T stringToObject(String string, Class<T> clazz) throws IOException {
        return objectMapper.readValue(
                string,
                clazz);
    }

    public static <T> T stringToObject(byte[] data, Class<T> clazz) throws IOException {
        return objectMapper.readValue(
                new String(data, StandardCharsets.UTF_8),
                clazz);
    }

    public static <T> T fileToObject(File file, Class<T> clazz) throws IOException {
        return objectMapper.readValue(file, clazz);
    }

    public static <T> T fileToObject(InputStream is, Class<T> clazz) throws IOException {
        return objectMapper.readValue(is, clazz);
    }

    public static void objectToFile(String filePath, Object o) throws IOException {
        objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(new File(filePath), o);
    }

    public static String asJsonString(final Object obj) throws JsonProcessingException {
        return objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(obj);
    }
    public static Map<String,Object> asMap(final Object obj) throws JsonProcessingException {
        String str = asJsonString(obj);
        return objectMapper
                .readValue(asJsonString(obj), new TypeReference<>() {
                });
    }
    public static Map<String,String> asMapOfStrings(final Object obj) throws JsonProcessingException {
        String str = asJsonString(obj);
        return objectMapper
                .readValue(asJsonString(obj), new TypeReference<>() {
                });
    }

}
