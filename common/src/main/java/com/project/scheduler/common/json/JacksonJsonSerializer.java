package com.project.scheduler.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JacksonJsonSerializer implements JsonSerializer {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  public String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new JsonSerializationException("serialize", exception);
    }
  }

  public <T> T fromJson(String json, Class<T> targetType) {
    try {
      return mapper.readValue(json, targetType);
    } catch (JsonProcessingException exception) {
      throw new JsonSerializationException("deserialize", exception);
    }
  }
}
