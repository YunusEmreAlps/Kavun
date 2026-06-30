package com.kavun.shared.util.core;

import java.util.regex.Pattern;

/**
 * Utility methods for common string operations used across the application.
 *
 * @author Yunus Emre Alpu
 * @version 1.1
 * @since 1.0
 */
public interface StringUtils {

  Pattern SAFE_CHARACTERS_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");

  static boolean hasUnsafeCharacters(String input) {
    if (input == null || input.isBlank()) {
      return true;
    }
    return !SAFE_CHARACTERS_PATTERN.matcher(input).matches();
  }

  static String truncate(String str, int maxLength) {
    if (str == null || maxLength < 0 || str.length() <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength) + "...";
  }

  static String nullSafe(String value) {
    return nullSafe(value, "");
  }

  static String nullSafe(String value, String defaultValue) {
    return value != null ? value : defaultValue;
  }

  static String capitalize(String s) {
    if (s == null || s.isEmpty()) {
      return s;
    }
    char first = s.charAt(0);
    char upper = Character.toUpperCase(first);
    return upper == first ? s : upper + s.substring(1);
  }
}
