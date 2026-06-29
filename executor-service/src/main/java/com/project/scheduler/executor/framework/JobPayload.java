package com.project.scheduler.executor.framework;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobPayload(
    String type,
    String url,
    String method,
    Map<String, String> headers,
    String body,
    String command,
    String to,
    String subject) {}
