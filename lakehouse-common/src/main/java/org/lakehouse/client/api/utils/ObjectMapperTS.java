package org.lakehouse.client.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectMapperTS extends ObjectMapper {
    public ObjectMapperTS() {
        this.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.registerModule(new JavaTimeModule());
    }
}
