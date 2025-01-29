package org.lakehouse.client.api.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;


public  class ObjectMapping {
	private static final ObjectMapper objectMapper = new ObjectMapper();
    
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
    
    public static void objectTofile(String filePath, Object o) throws StreamWriteException, DatabindException, IOException {
    	objectMapper
    		.writerWithDefaultPrettyPrinter()
    		.writeValue(new File(filePath),o);
    }
	public static String asJsonString(final Object obj) throws JsonProcessingException {
			return objectMapper
					.writerWithDefaultPrettyPrinter()
					.writeValueAsString(obj);	
	}
}
