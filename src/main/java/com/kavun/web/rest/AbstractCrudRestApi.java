package com.kavun.web.rest;

import com.kavun.annotation.Loggable;
import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.base.BaseService;
import com.kavun.constant.PermissionConstants.Actions;
import com.kavun.shared.dto.BaseDto;
import com.kavun.shared.request.BaseRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The standard CRUD endpoints of a resource, backed by a {@link BaseService}. A resource controller
 * only adds its mapping, tag and page:
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/v1/role")
 * @Tag(name = "03. Role Management")
 * @RequirePermission(page = Pages.ROLES)
 * public class RoleRestApi extends AbstractCrudRestApi<RoleRequest, RoleDto> {
 *   public RoleRestApi(RoleService roleService) {
 *     super(roleService);
 *   }
 * }
 * }</pre>
 *
 * <p>Filters are passed as query parameters (or as the body of {@code POST /search}) and are
 * applied by the service to the fields it declares filterable. Soft deleted records are only
 * listed with {@code deleted=true}.
 *
 * @param <REQUEST> request type for create and update
 * @param <DTO> response type
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractCrudRestApi<REQUEST extends BaseRequest, DTO extends BaseDto> {

  protected final BaseService<REQUEST, DTO> service;

  protected AbstractCrudRestApi(BaseService<REQUEST, DTO> service) {
    this.service = service;
  }

  @Loggable
  @GetMapping
  @Operation(summary = "List records", description = "Paged list, filtered by query parameters")
  public Page<DTO> getAll(
      @RequestParam Map<String, String> filters,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return service.findAll(filters, pageable);
  }

  @Loggable
  @GetMapping("/list")
  @Operation(summary = "List all records", description = "Unpaged list, filtered by query parameters")
  public List<DTO> getAllList(@RequestParam Map<String, String> filters) {
    return service.findAll(filters);
  }

  @Loggable
  @GetMapping("/{id}")
  @Operation(summary = "Get a record by id")
  public DTO getById(@PathVariable Long id) {
    return service.findById(id);
  }

  @Loggable
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a record")
  public DTO create(@Valid @RequestBody REQUEST request) {
    return service.create(request);
  }

  @Loggable
  @PutMapping("/{id}")
  @Operation(summary = "Update a record")
  public DTO update(@PathVariable Long id, @Valid @RequestBody REQUEST request) {
    return service.update(id, request);
  }

  @Loggable
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete a record", description = "Soft delete; the record can be restored")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  // Restoring undoes a delete, so it needs the same permission.
  @Loggable
  @RequirePermission(action = Actions.DELETE)
  @PostMapping("/{id}/restore")
  @Operation(summary = "Restore a deleted record")
  public DTO restore(@PathVariable Long id) {
    return service.restore(id);
  }

  @Loggable
  @GetMapping("/{id}/exists")
  @Operation(summary = "Check whether a record exists")
  public boolean existsById(@PathVariable Long id) {
    return service.existsById(id);
  }

  @Loggable
  @GetMapping("/count")
  @Operation(summary = "Count records", description = "Filtered by query parameters")
  public long count(@RequestParam Map<String, String> filters) {
    return service.count(filters);
  }

  // A read-only POST, for filters that do not fit in a query string.
  @Loggable
  @RequirePermission(action = Actions.VIEW)
  @PostMapping("/search")
  @Operation(summary = "Search records", description = "Paged list, filtered by the request body")
  public Page<DTO> search(
      @RequestBody Map<String, Object> filters,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return service.findAll(filters, pageable);
  }
}
