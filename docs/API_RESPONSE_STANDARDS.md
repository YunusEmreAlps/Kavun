# Enterprise API Response Standards

## Overview

This document describes the standardized API response format used across the **Kavun** application. All API endpoints follow consistent response structures for better client integration and debugging.

## Response Structure

### Success Response

```json
{
  "timestamp": "2025-11-18T10:30:45.123+03:00",
  "status": 200,
  "code": "SUCCESS",
  "message": "Resource retrieved successfully",
  "data": {
    "id": 1,
    "username": "admin"
  },
  "path": "/api/v1/users/1"
}
```

### Error Response

```json
{
  "timestamp": "2025-11-18T10:30:45.123+03:00",
  "status": 404,
  "code": "NOT_FOUND",
  "message": "Resource not found",
  "path": "/api/v1/users/999"
}
```

### Validation Error Response

```json
{
  "timestamp": "2025-11-18T10:30:45.123+03:00",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": {
    "email": ["Email format is invalid"],
    "password": ["Password must be at least 8 characters"]
  },
  "path": "/api/v1/auth/register"
}
```

## Response Codes

### Success Codes (2xx)

| Code       | HTTP Status | Description                         |
|---------   |-------------|-------------------------------------|
| SUCCESS    | 200         | Generic success operation           |
| CREATED    | 201         | Resource created successfully       |
| UPDATED    | 200         | Resource updated successfully       |
| DELETED    | 200         | Resource deleted successfully       |
| RETRIEVED  | 200         | Resource retrieved successfully     |
| NO_CONTENT | 204         | Operation completed with no content |

### Client Error Codes (4xx)

| Code             | HTTP Status | Description                 |
|------------------|-------------|-----------------------------|
| BAD_REQUEST      | 400         | Invalid request parameters  |
| UNAUTHORIZED     | 401         | Authentication required     |
| FORBIDDEN        | 403         | Access denied               |
| NOT_FOUND        | 404         | Resource not found          |
| CONFLICT         | 409         | Resource conflict detected  |
| VALIDATION_ERROR | 400         | Validation failed           |
| ALREADY_EXISTS   | 409         | Resource already exists     |

### Server Error Codes (5xx)

| Code                | HTTP Status | Description                     |
|---------------------|-------------|---------------------------------|
| INTERNAL_ERROR      | 500         | Internal server error           |
| DATABASE_ERROR      | 500         | Database operation failed       |
| SERVICE_UNAVAILABLE | 503         | Service temporarily unavailable |

## Usage in Controllers

### Automatic Wrapping

All controller responses are automatically wrapped by `ResponseHandler`. Simply return your data:

```java
@GetMapping("/{id}")
public UserDto getUser(@PathVariable Long id) {
    return userService.findById(id); // Automatically wrapped
}
```

### Manual Control with ApiResponse

For custom messages or error handling:

```java
@PostMapping
public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody UserDto dto, HttpServletRequest request) {
    UserDto created = userService.create(dto);
    return ApiResponse.success(ResponseCode.CREATED, created, request.getRequestURI())
        .toResponseEntity();
}
```

### Error Handling

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id, HttpServletRequest request) {
    Optional<UserDto> user = userService.findById(id);
    
    if (user.isEmpty()) {
        return ApiResponse.error(
            ResponseCode.NOT_FOUND,
            "User not found with id: " + id,
            request.getRequestURI()
        ).toResponseEntity();
    }
    
    return ApiResponse.success(ResponseCode.RETRIEVED, user.get(), request.getRequestURI())
        .toResponseEntity();
}
```

## Best Practices

### 1. Use Appropriate Response Codes

Match the response code to the operation:

- `CREATED` for POST operations
- `UPDATED` for PUT/PATCH operations
- `DELETED` for DELETE operations
- `RETRIEVED` for GET operations

### 2. Provide Meaningful Messages

```java
// Good
ApiResponse.error(ResponseCode.NOT_FOUND, "User with email 'admin@example.com' not found", path)

// Bad
ApiResponse.error(ResponseCode.NOT_FOUND, "Not found", path)
```

### 3. Include Path Information

Always include the request path for better debugging:

```java
ApiResponse.success(ResponseCode.SUCCESS, data, request.getRequestURI())
```

### 4. Handle Validation Properly

Use `@Valid` annotations and let the global handler create validation error responses automatically:

```java
@PostMapping
public UserDto createUser(@Valid @RequestBody UserDto dto) {
    return userService.create(dto);
    // Validation errors automatically handled
}
```

### 5. Log Errors Appropriately

The global exception handler logs all errors. No need to duplicate logging in controllers.

## Exception Handling

### Automatic Exception Mapping

The following exceptions are automatically handled:

- `IllegalArgumentException` → CONFLICT (409)
- `IllegalStateException` → CONFLICT (409)
- `EntityNotFoundException` → NOT_FOUND (404)
- `AccessDeniedException` → FORBIDDEN (403)
- `MethodArgumentNotValidException` → VALIDATION_ERROR (400)
- `Exception` (fallback) → INTERNAL_ERROR (500)

### Custom Exceptions

Create domain-specific exceptions for better error handling:

```java
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("User already exists with email: " + email);
    }
}

// In exception handler
@ExceptionHandler(UserAlreadyExistsException.class)
protected ResponseEntity<ApiResponse<Object>> handleUserExists(
        UserAlreadyExistsException ex, HttpServletRequest request) {
    return ApiResponse.error(
        ResponseCode.ALREADY_EXISTS,
        ex.getMessage(),
        request.getRequestURI()
    ).toResponseEntity();
}
```

## Testing

### Example Test

```java
@Test
void testGetUser_Success() {
    mockMvc.perform(get("/api/v1/users/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("RETRIEVED"))
        .andExpect(jsonPath("$.data.id").value(1))
        .andExpect(jsonPath("$.path").value("/api/v1/users/1"));
}

@Test
void testGetUser_NotFound() {
    mockMvc.perform(get("/api/v1/users/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").exists());
}
```

## Migration from CustomResponse

If you have existing code using `CustomResponse`, it will still work. Both formats are supported:

```java
// Old way (still works)
return CustomResponse.of(HttpStatus.OK, data, "Success", path);

// New way (recommended)
return ApiResponse.success(ResponseCode.SUCCESS, data, path);
```

**Note:** `CustomResponse` has been removed from the codebase.

## Client Integration

### TypeScript Interface

```typescript
interface ApiResponse<T> {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  data?: T;
  path: string;
  errors?: Record<string, string[]>;
  metadata?: Record<string, any>;
}
```

### JavaScript Example

```javascript
fetch('/api/v1/users/1')
  .then(response => response.json())
  .then(apiResponse => {
    if (apiResponse.status >= 200 && apiResponse.status < 300) {
      console.log('Success:', apiResponse.data);
    } else {
      console.error('Error:', apiResponse.message);
    }
  });
```

## Support

For questions or issues with the API response format, contact the backend team or create an issue in the project repository.
