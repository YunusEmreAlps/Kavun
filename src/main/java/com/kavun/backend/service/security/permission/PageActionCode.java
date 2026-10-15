package com.kavun.backend.service.security.permission;

import org.apache.commons.lang3.StringUtils;

/**
 * A permission identifier: an action on a page, written as {@code PAGE_CODE:ACTION_CODE}.
 *
 * @param page the page code
 * @param action the action code
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public record PageActionCode(String page, String action) {

  private static final String SEPARATOR = ":";

  public PageActionCode {
    if (StringUtils.isBlank(page) || StringUtils.isBlank(action)) {
      throw new IllegalArgumentException("Page and action codes must not be blank");
    }
  }

  /**
   * Parses a {@code PAGE_CODE:ACTION_CODE} string.
   *
   * @param value the value to parse
   * @return the page action code
   * @throws IllegalArgumentException if the value is not in the expected format
   */
  public static PageActionCode parse(String value) {
    String[] parts = StringUtils.defaultString(value).split(SEPARATOR, -1);
    if (parts.length != 2) {
      throw new IllegalArgumentException(
          "Expected 'PAGE_CODE:ACTION_CODE' but got '" + value + "'");
    }
    return new PageActionCode(parts[0].trim(), parts[1].trim());
  }

  @Override
  public String toString() {
    return page + SEPARATOR + action;
  }
}
