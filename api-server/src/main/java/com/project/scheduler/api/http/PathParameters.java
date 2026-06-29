package com.project.scheduler.api.http;

import java.util.*;
import java.util.regex.*;

final class PathParameters {
  private PathParameters() {}

  static Map<String, String> from(Matcher matcher) {
    Map<String, String> values = new HashMap<>();
    for (String parameterName : new String[] {"id"})
      try {
        String parameterValue = matcher.group(parameterName);
        if (parameterValue != null) values.put(parameterName, parameterValue);
      } catch (IllegalArgumentException ignored) {
      }
    return values;
  }
}
