package com.kavun.web.payload.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import com.kavun.constant.base.BaseConstants;

/**
 * Standardized response codes for API responses.
 * Follows enterprise best practices with unique codes for each operation.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
public enum ResponseCode {
  // Success codes (2xx)
  SUCCESS("SUCCESS", BaseConstants.OPERATION_SUCCESSFUL, HttpStatus.OK),
  CREATED("CREATED", BaseConstants.RESOURCE_CREATED, HttpStatus.OK),
  UPDATED("UPDATED", BaseConstants.RESOURCE_UPDATED, HttpStatus.OK),
  DELETED("DELETED", BaseConstants.RESOURCE_DELETED, HttpStatus.OK),
  RETRIEVED("RETRIEVED", BaseConstants.RESOURCE_RETRIEVED, HttpStatus.OK),
  NO_CONTENT("NO_CONTENT", BaseConstants.NO_CONTENT, HttpStatus.OK),

  // Client error codes (4xx)
  BAD_REQUEST("BAD_REQUEST", BaseConstants.INVALID_REQUEST_PARAMETERS, HttpStatus.BAD_REQUEST),
  UNAUTHORIZED("UNAUTHORIZED", BaseConstants.AUTHENTICATION_REQUIRED, HttpStatus.UNAUTHORIZED),
  FORBIDDEN("FORBIDDEN", BaseConstants.ACCESS_DENIED, HttpStatus.FORBIDDEN),
  NOT_FOUND("NOT_FOUND", BaseConstants.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND),
  CONFLICT("CONFLICT", BaseConstants.RESOURCE_CONFLICT, HttpStatus.CONFLICT),
  VALIDATION_ERROR("VALIDATION_ERROR", BaseConstants.VALIDATION_FAILED, HttpStatus.BAD_REQUEST),
  ALREADY_EXISTS("ALREADY_EXISTS", BaseConstants.RESOURCE_ALREADY_EXISTS, HttpStatus.CONFLICT),
  DATA_INTEGRITY_VIOLATION("DATA_INTEGRITY_VIOLATION", BaseConstants.DATA_INTEGRITY_VIOLATION, HttpStatus.BAD_REQUEST),
  INVALID_JSON("INVALID_JSON", BaseConstants.INVALID_JSON_REQUEST, HttpStatus.BAD_REQUEST),
  RESOURCE_IN_USE("RESOURCE_IN_USE", BaseConstants.RESOURCE_CANNOT_BE_DELETED, HttpStatus.BAD_REQUEST),
  METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", BaseConstants.METHOD_NOT_ALLOWED, HttpStatus.METHOD_NOT_ALLOWED),
  NOT_ACCEPTABLE("NOT_ACCEPTABLE", BaseConstants.NOT_ACCEPTABLE, HttpStatus.NOT_ACCEPTABLE),
  UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", BaseConstants.UNSUPPORTED_MEDIA_TYPE,
      HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  TOO_MANY_REQUESTS("TOO_MANY_REQUESTS", BaseConstants.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS),

  // Server error codes (5xx)
  INTERNAL_ERROR("INTERNAL_ERROR", BaseConstants.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
  DATABASE_ERROR("DATABASE_ERROR", BaseConstants.DATABASE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
  SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", BaseConstants.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
  PAYLOAD_TOO_LARGE("PAYLOAD_TOO_LARGE", BaseConstants.PAYLOAD_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;

  ResponseCode(String code, String message, HttpStatus httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }

  /**
   * Get the HTTP status code value.
   */
  public int getStatusValue() {
    return httpStatus.value();
  }
}
