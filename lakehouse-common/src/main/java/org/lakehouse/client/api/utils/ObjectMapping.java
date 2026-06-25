/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.client.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ObjectMapping {
    private static final ObjectMapper objectMapper = new ObjectMapperTS();

    static {
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

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


    public static <T> T mapToObject(Map<?,?> map, Class<T> clazz) throws IOException {
        return objectMapper.convertValue(map, clazz);
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
                .writeValueAsString(obj);
    }
    public static String asJsonStringPretty(final Object obj) throws JsonProcessingException {
        return objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(obj);
    }
    public static Map<String,Object> asMap(final Object obj) throws JsonProcessingException {
        String str = asJsonStringPretty(obj);
        return objectMapper
                .readValue(asJsonStringPretty(obj), new TypeReference<>() {
                });
    }
    public static Map<String,String> asMapOfStrings(final Object obj) throws JsonProcessingException {
        String str = asJsonStringPretty(obj);
        return objectMapper
                .readValue(asJsonStringPretty(obj), new TypeReference<>() {
                });
    }



}
