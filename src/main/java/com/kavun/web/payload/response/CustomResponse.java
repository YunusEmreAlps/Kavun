package com.kavun.web.payload.response;

import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response wrapper for API endpoints.
 *
 * @param <T> the type of the data to be returned
 * @author Yunus Emre Alpu
 * @version 1.1
 * @since 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomResponse<T> {

  /** The timestamp when the response is created. */
  @Builder.Default
  private ZonedDateTime timestamp = ZonedDateTime.now();

  /** The HTTP status code of the response. */
  private int status;

  /** The data to be returned in the response, can be null. */
  @Builder.Default
  private Optional<T> data = Optional.empty();

  /** The message to be returned in the response, can be null. */
  private String message;

  /** The path of the endpoint that produced the response. */
  private String path;

  /**
   * Static factory method to create a CustomResponse with data.
   *
   * @param status  the HTTP status code
   * @param data    the data to be returned
   * @param message the message to be returned, can be null
   * @param path    the path of the endpoint
   * @param <T>     the type of the data
   * @return a new CustomResponse instance
   */
  public static <T> CustomResponse<T> of(HttpStatus status, T data, String message, String path) {
    return CustomResponse.<T>builder()
        .status(status.value())
        .data(Optional.ofNullable(data))
        .message(message)
        .path(path)
        .build();
  }

  /**
   * Static factory method to create a CustomResponse without data.
   *
   * @param status  the HTTP status code
   * @param message the message to be returned, can be null
   * @param path    the path of the endpoint
   * @param <T>     the type of the data
   * @return a new CustomResponse instance
   */
  public static <T> CustomResponse<T> of(HttpStatus status, String message, String path) {
    return CustomResponse.<T>builder()
        .status(status.value())
        .message(message)
        .path(path)
        .build();
  }

  /**
   * Static factory method for minimal response with only data and message.
   *
   * @param data    the data to be returned
   * @param message the message to be returned, can be null
   * @param <T>     the type of the data
   * @return a new CustomResponse instance
   */
  public static <T> CustomResponse<T> minimal(T data, String message) {
    return CustomResponse.<T>builder()
        .data(Optional.ofNullable(data))
        .message(message)
        .build();
  }

  /**
   * Converts this CustomResponse to a ResponseEntity.
   *
   * @return a ResponseEntity containing this CustomResponse
   */
  /*public ResponseEntity<CustomResponse<T>> toResponseEntity() {
    return ResponseEntity.status(status).body(this);
  }*/

  /**
   * Static factory method to create a CustomResponse with data and status code.
   *
   * @param response the response to be returned
   * @param path     the path of the endpoint
   * @param <U>      the type of the data
   * @return a new ResponseEntity instance
   */
  public <U> ResponseEntity<CustomResponse<U>> toResponseEntity(CustomResponse<U> response, String path) {
    if (response == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
          CustomResponse.of(HttpStatus.NOT_FOUND, "Not Found", path));
    }
    response.setPath(path);
    return ResponseEntity.ok(response);
  }

  /**
   * Creates a CustomResponse for an error scenario.
   *
   * @param status  the HTTP status code
   * @param message the error message, can be null
   * @param path    the path of the endpoint
   * @param <T>     the type of the data
   * @return a new CustomResponse instance for errors
   */
  public static <T> CustomResponse<T> error(HttpStatus status, String message, String path) {
    return CustomResponse.<T>builder()
        .status(status.value())
        .message(message)
        .path(path)
        .build();
  }
}
