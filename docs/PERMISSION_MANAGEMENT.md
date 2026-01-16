# Permission Management with @RequirePermission

## Overview

The `@RequirePermission` annotation provides a flexible and powerful way to manage endpoint permissions in the application. It supports both hardcoded permission definitions and dynamic auto-detection based on HTTP methods, URL paths, and custom headers.

## Table of Contents

- [Basic Concepts](#basic-concepts)
- [Annotation Parameters](#annotation-parameters)
- [Auto-Detection Mechanism](#auto-detection-mechanism)
- [Usage Examples](#usage-examples)
- [Priority Rules](#priority-rules)
- [Frontend Integration](#frontend-integration)
- [Best Practices](#best-practices)

## Basic Concepts

### Permission Format

Permissions follow the format: `PAGE_CODE:ACTION_CODE`

- **PAGE_CODE**: Identifies the page or module (e.g., `ACTIONS`, `USERS`, `REPORTS`)
- **ACTION_CODE**: Identifies the action (e.g., `VIEW`, `CREATE`, `EDIT`, `DELETE`, `APPROVE`)

Example: `ACTIONS:DELETE` means "delete permission on the actions page"

### Permission Check Logic

The system uses **OR logic** for multiple permissions - users need **any one** of the specified permissions to access an endpoint.

## Annotation Parameters

### `pageActions` (String[])

Explicitly defines required permissions.

```java
@RequirePermission(pageActions = { "ACTIONS:DELETE", "ADMIN:DELETE" })
```

- Default: `{}`
- Use when you need explicit control over permissions
- Multiple values create OR logic (user needs any one)

### `autoDetect` (boolean)

Enables automatic permission detection from HTTP method, URL path, and headers.

```java
@RequirePermission(autoDetect = true)
```

- Default: `false`
- Only works when `pageActions` is empty
- See [Auto-Detection Mechanism](#auto-detection-mechanism) for details

### `actionOverride` (String)

Overrides the action part when using `autoDetect`.

```java
@RequirePermission(autoDetect = true, actionOverride = "APPROVE")
```

- Default: `""`
- Useful when HTTP method doesn't match the actual business action
- Example: POST request requiring APPROVE permission instead of CREATE

### `message` (String)

Custom error message when permission is denied.

```java
@RequirePermission(
    autoDetect = true,
    message = "You don't have permission to approve actions"
)
```

- Default: `"Bu işlemi gerçekleştirmek için izniniz bulunmamaktadır."`

## Auto-Detection Mechanism

When `autoDetect = true` and `pageActions` is empty, the system automatically determines permissions using:

### 1. Page Code Detection

**Priority Order:**

1. **`Page-Code` Header** (from frontend)
   ```javascript
   fetch('/api/v1/action/delete/123', {
     headers: { 'Page-Code': 'CUSTOM_PAGE' }
   });
   // Result: CUSTOM_PAGE:DELETE
   ```

2. **URL Path Extraction**
   - Pattern: `/api/v1/{resource}/...`
   - Example: `/api/v1/action/delete/123` → `ACTION`
   - Example: `/api/v1/user/list` → `USER`

### 2. Action Code Detection

**Priority Order:**

1. **`actionOverride` Parameter**
   ```java
   @RequirePermission(autoDetect = true, actionOverride = "APPROVE")
   // Result: PAGE_CODE:APPROVE
   ```

2. **HTTP Method Mapping**
   - `GET` → `VIEW`
   - `POST` → `CREATE`
   - `PUT` / `PATCH` → `EDIT`
   - `DELETE` → `DELETE`

## Usage Examples

### Standard REST Operations

```java
@RestController
@RequestMapping("/api/v1/action")
public class ActionRestApi {

    // GET → ACTION:VIEW
    @GetMapping("/{id}")
    @RequirePermission(autoDetect = true)
    public ActionDto getById(@PathVariable UUID id) {
        return actionService.get(id);
    }

    // POST → ACTION:CREATE
    @PostMapping("/create")
    @RequirePermission(autoDetect = true)
    public ActionDto create(@RequestBody ActionRequest request) {
        return actionService.save(request);
    }

    // PUT → ACTION:EDIT
    @PutMapping("/update/{id}")
    @RequirePermission(autoDetect = true)
    public ActionDto update(@PathVariable UUID id, @RequestBody ActionRequest request) {
        return actionService.update(id, request);
    }

    // DELETE → ACTION:DELETE
    @DeleteMapping("/delete/{id}")
    @RequirePermission(autoDetect = true)
    public void delete(@PathVariable UUID id) {
        actionService.delete(id);
    }
}
```

### Custom Business Actions

```java
@RestController
@RequestMapping("/api/v1/action")
public class ActionRestApi {

    // POST but requires APPROVE permission
    @PostMapping("/approve/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "APPROVE")
    public ActionDto approve(@PathVariable UUID id) {
        return actionService.approve(id);
    }

    // POST but requires REJECT permission
    @PostMapping("/reject/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "REJECT")
    public ActionDto reject(@PathVariable UUID id) {
        return actionService.reject(id);
    }

    // POST but requires PUBLISH permission
    @PostMapping("/publish/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "PUBLISH")
    public ActionDto publish(@PathVariable UUID id) {
        return actionService.publish(id);
    }

    // GET but requires EXPORT permission
    @GetMapping("/export")
    @RequirePermission(autoDetect = true, actionOverride = "EXPORT")
    public ResponseEntity<byte[]> export() {
        return actionService.exportToExcel();
    }
}
```

### Hardcoded Permissions

```java
@RestController
@RequestMapping("/api/v1/action")
public class ActionRestApi {

    // User needs EITHER "ACTIONS:DELETE" OR "ADMIN:DELETE"
    @DeleteMapping("/delete/{id}")
    @RequirePermission(pageActions = { "ACTIONS:DELETE", "ADMIN:DELETE" })
    public void delete(@PathVariable UUID id) {
        actionService.delete(id);
    }

    // Complex scenario: multiple page permissions
    @PostMapping("/special-operation")
    @RequirePermission(
        pageActions = { 
            "WORKFLOW:APPROVE", 
            "ADMIN:SPECIAL_OPS",
            "SUPERVISOR:OVERRIDE" 
        },
        message = "You need supervisor-level permissions for this operation"
    )
    public void specialOperation(@RequestBody SpecialRequest request) {
        actionService.processSpecial(request);
    }
}
```

### Mixed Approaches

```java
@RestController
@RequestMapping("/api/v1/report")
public class ReportRestApi {

    // Auto-detect for standard operations
    @GetMapping("/{id}")
    @RequirePermission(autoDetect = true)  // REPORT:VIEW
    public ReportDto getById(@PathVariable UUID id) {
        return reportService.get(id);
    }

    // Hardcoded for sensitive operations
    @PostMapping("/financial-export")
    @RequirePermission(
        pageActions = { "FINANCIAL_REPORTS:EXPORT", "CFO:VIEW" },
        message = "Financial exports require special authorization"
    )
    public ResponseEntity<byte[]> exportFinancial() {
        return reportService.exportFinancial();
    }

    // actionOverride for special cases
    @PostMapping("/schedule/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "SCHEDULE")
    public ReportDto schedule(@PathVariable UUID id) {
        return reportService.schedule(id);
    }
}
```

## Priority Rules

### Decision Flow

```
1. Is pageActions specified and not empty?
   YES → Use hardcoded pageActions
   NO → Continue to step 2

2. Is autoDetect = true?
   YES → Continue to step 3
   NO → Throw error (no permissions specified)

3. Detect PAGE_CODE:
   a. Check Page-Code header
   b. If not found, extract from URL path
   c. If not found, throw error

4. Detect ACTION_CODE:
   a. Check actionOverride parameter
   b. If not set, map from HTTP method
   c. If cannot map, throw error

5. Result: PAGE_CODE:ACTION_CODE
```

### Example Flow

For request: `POST /api/v1/action/approve/123`

```java
@PostMapping("/approve/{id}")
@RequirePermission(autoDetect = true, actionOverride = "APPROVE")
public ActionDto approve(@PathVariable UUID id) { ... }
```

**Detection Steps:**
1. pageActions is empty? **YES** → Continue
2. autoDetect is true? **YES** → Continue
3. Page-Code header? **Not provided**
4. Extract from URL path: `/api/v1/action/...` → `ACTION`
5. actionOverride? **APPROVE**
6. **Result:** `ACTION:APPROVE`

## Frontend Integration

### Sending Page-Code Header

```javascript
// Using Fetch API
fetch('/api/v1/action/delete/123', {
  method: 'DELETE',
  headers: {
    'Page-Code': 'WORKFLOW_ACTIONS',  // Custom page code
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + token
  }
});

// Using Axios
axios.delete('/api/v1/action/delete/123', {
  headers: {
    'Page-Code': 'WORKFLOW_ACTIONS'
  }
});

// Using Axios Interceptor (Global)
axios.interceptors.request.use(config => {
  // Auto-detect page from current route
  const currentPage = getCurrentPageCode(); // Your logic here
  config.headers['Page-Code'] = currentPage;
  return config;
});
```

### Dynamic Page Code Example

```javascript
// React Example
import { useLocation } from 'react-router-dom';
import axios from 'axios';

const useApiClient = () => {
  const location = useLocation();
  
  const getPageCode = () => {
    // Map route to page code
    const pageMap = {
      '/dashboard': 'DASHBOARD',
      '/users': 'USERS',
      '/actions': 'ACTIONS',
      '/reports': 'REPORTS'
    };
    
    const basePath = '/' + location.pathname.split('/')[1];
    return pageMap[basePath] || 'DEFAULT';
  };

  const apiClient = axios.create({
    baseURL: '/api/v1',
    headers: {
      'Page-Code': getPageCode()
    }
  });

  return apiClient;
};

// Usage
const api = useApiClient();
api.delete(`/action/delete/${id}`); // Automatically includes Page-Code
```

## Best Practices

### 1. Choose the Right Approach

**Use `autoDetect = true` when:**
- Standard REST CRUD operations
- Action matches HTTP method semantics
- Page can be derived from URL or header

**Use `actionOverride` when:**
- Business action differs from HTTP method
- Example: POST for approve/reject/publish operations

**Use hardcoded `pageActions` when:**
- Complex permission requirements
- Multiple alternative permissions (OR logic)
- Cross-page permissions needed
- Sensitive operations requiring explicit control

### 2. Naming Conventions

**Page Codes:**
- Use UPPER_SNAKE_CASE
- Be descriptive but concise
- Examples: `USERS`, `ACTIONS`, `FINANCIAL_REPORTS`, `USER_MANAGEMENT`

**Action Codes:**
- Use UPPER_SNAKE_CASE
- Standard actions: `VIEW`, `CREATE`, `EDIT`, `DELETE`
- Custom actions: `APPROVE`, `REJECT`, `PUBLISH`, `EXPORT`, `IMPORT`, `SCHEDULE`

### 3. Documentation

Always document non-standard permissions:

```java
/**
 * Approve an action.
 * 
 * Required Permission: ACTION:APPROVE
 * This is a supervisor-level action that requires explicit approval rights.
 * 
 * @param id Action ID to approve
 * @return Updated action
 */
@PostMapping("/approve/{id}")
@RequirePermission(autoDetect = true, actionOverride = "APPROVE")
public ActionDto approve(@PathVariable UUID id) {
    return actionService.approve(id);
}
```

### 4. Admin Bypass Configuration

Configure admin bypass in `application.properties`:

```properties
# Enable/disable admin bypass for permission checks
security.permission.admin-bypass-enabled=true
```

When enabled, users with `ROLE_ADMIN` automatically bypass all permission checks.

### 5. Logging and Debugging

The system logs detailed permission check information:

```
DEBUG - Checking permission for user abc123 on DELETE /api/v1/action/delete/123 (Page-Code header: ACTIONS)
DEBUG - RequirePermission pageActions: [], autoDetect: true
INFO  - Auto-detected page:action: ACTIONS:DELETE
DEBUG - Checking if user has any of page:action combinations: ACTIONS:DELETE
DEBUG - User abc123 granted access via page:action permission
```

Enable debug logging:

```properties
logging.level.com.boss.annotation.impl.PermissionAspect=DEBUG
```

### 6. Error Handling

Provide meaningful error messages:

```java
@RequirePermission(
    pageActions = { "FINANCIAL_REPORTS:EXPORT" },
    message = "Only authorized financial staff can export this data. Contact your manager for access."
)
```

### 7. Testing Permissions

```java
@Test
@WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
void testDeleteWithoutPermission() {
    // Should throw AccessDeniedException
    assertThrows(AccessDeniedException.class, () -> {
        actionRestApi.delete(actionId);
    });
}

@Test
@WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
void testDeleteAsAdmin() {
    // Should succeed (admin bypass)
    assertDoesNotThrow(() -> {
        actionRestApi.delete(actionId);
    });
}

@Test
@WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
void testDeleteWithPermission() {
    // Mock user having ACTIONS:DELETE permission
    when(permissionCheckService.hasPermissionByUserId(any(), any()))
        .thenReturn(true);
    
    assertDoesNotThrow(() -> {
        actionRestApi.delete(actionId);
    });
}
```

## Common Patterns

### Pattern 1: Standard CRUD Controller

```java
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductRestApi {

    private final ProductService productService;

    @GetMapping("/{id}")
    @RequirePermission(autoDetect = true)  // PRODUCT:VIEW
    public ProductDto getById(@PathVariable UUID id) {
        return productService.get(id);
    }

    @GetMapping("/paging")
    @RequirePermission(autoDetect = true)  // PRODUCT:VIEW
    public Page<ProductDto> paging(Pageable pageable) {
        return productService.paging(pageable);
    }

    @PostMapping("/create")
    @RequirePermission(autoDetect = true)  // PRODUCT:CREATE
    public ProductDto create(@RequestBody ProductRequest request) {
        return productService.save(request);
    }

    @PutMapping("/update/{id}")
    @RequirePermission(autoDetect = true)  // PRODUCT:EDIT
    public ProductDto update(@PathVariable UUID id, @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/delete/{id}")
    @RequirePermission(autoDetect = true)  // PRODUCT:DELETE
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}
```

### Pattern 2: Workflow Controller

```java
@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
public class WorkflowRestApi {

    private final WorkflowService workflowService;

    @GetMapping("/{id}")
    @RequirePermission(autoDetect = true)  // WORKFLOW:VIEW
    public WorkflowDto getById(@PathVariable UUID id) {
        return workflowService.get(id);
    }

    @PostMapping("/submit/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "SUBMIT")  // WORKFLOW:SUBMIT
    public WorkflowDto submit(@PathVariable UUID id) {
        return workflowService.submit(id);
    }

    @PostMapping("/approve/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "APPROVE")  // WORKFLOW:APPROVE
    public WorkflowDto approve(@PathVariable UUID id) {
        return workflowService.approve(id);
    }

    @PostMapping("/reject/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "REJECT")  // WORKFLOW:REJECT
    public WorkflowDto reject(@PathVariable UUID id, @RequestBody RejectionReason reason) {
        return workflowService.reject(id, reason);
    }

    @PostMapping("/cancel/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "CANCEL")  // WORKFLOW:CANCEL
    public WorkflowDto cancel(@PathVariable UUID id) {
        return workflowService.cancel(id);
    }
}
```

### Pattern 3: Mixed Permission Controller

```java
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportRestApi {

    private final ReportService reportService;

    // Standard operations use auto-detect
    @GetMapping("/{id}")
    @RequirePermission(autoDetect = true)
    public ReportDto getById(@PathVariable UUID id) {
        return reportService.get(id);
    }

    // Special operations use actionOverride
    @GetMapping("/export/{id}")
    @RequirePermission(autoDetect = true, actionOverride = "EXPORT")
    public ResponseEntity<byte[]> export(@PathVariable UUID id) {
        return reportService.export(id);
    }

    // Sensitive operations use hardcoded permissions with OR logic
    @PostMapping("/financial-audit")
    @RequirePermission(
        pageActions = { "FINANCIAL_REPORTS:AUDIT", "COMPLIANCE:AUDIT", "CFO:VIEW" },
        message = "Financial audit access requires special authorization"
    )
    public AuditReportDto financialAudit(@RequestBody AuditRequest request) {
        return reportService.performFinancialAudit(request);
    }
}
```

## Troubleshooting

### Permission Denied Unexpectedly

1. **Check debug logs** to see what permission was checked
2. **Verify user has the required permission** in database
3. **Check admin bypass setting** if user is admin
4. **Verify Page-Code header** if using frontend header
5. **Check URL path structure** for auto-detection

### Auto-Detection Not Working

1. **Verify `autoDetect = true`** in annotation
2. **Ensure `pageActions` is empty** (not specified)
3. **Check URL follows pattern** `/api/v1/{resource}/...`
4. **Review logs** for detection errors

### Wrong Permission Being Checked

1. **Check `actionOverride`** if action doesn't match HTTP method
2. **Verify Page-Code header** value
3. **Review URL path** for correct resource name
4. **Check method mapping** annotation (GET, POST, etc.)

## Migration Guide

### From Hardcoded to Auto-Detect

**Before:**
```java
@RequirePermission(pageActions = { "ACTIONS:VIEW" })
@GetMapping("/{id}")
public ActionDto getById(@PathVariable UUID id) { ... }
```

**After:**
```java
@RequirePermission(autoDetect = true)
@GetMapping("/{id}")
public ActionDto getById(@PathVariable UUID id) { ... }
```

**Steps:**
1. Replace `pageActions` with `autoDetect = true`
2. Test endpoints to verify correct permission detection
3. Add `actionOverride` if action doesn't match HTTP method
4. Update frontend to send `Page-Code` header if needed

## Related Documentation

- [Authentication](AUTHENTICATION.md) - User authentication setup
- [Security](SECURITY.md) - Overall security configuration
- [API Response Standards](API_RESPONSE_STANDARDS.md) - API conventions
- [Tests](TESTS.md) - Testing guidelines

## Version History

- **v1.1** - Added `autoDetect` and `actionOverride` parameters for flexible permission management
- **v1.0** - Initial implementation with hardcoded `pageActions`
