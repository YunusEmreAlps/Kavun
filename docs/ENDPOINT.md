# Adding a Resource

A resource (e.g. `Product`) with list, get, create, update, soft delete and restore endpoints takes
seven small files. The generic pieces (`AbstractService`, `AbstractCrudRestApi`,
`FilterSpecification`) do the rest.

## 1. Entity

```java
@Entity
@Table(name = "products")
@Getter
@Setter
@SQLDelete(sql = "UPDATE products SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
public class Product extends BaseEntity<Long> {

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false)
  private BigDecimal price;

  @ManyToOne(fetch = FetchType.LAZY)
  private Category category;
}
```

`BaseEntity` provides the id, `publicId`, version, audit fields, soft delete flag and the entity
identity (`equals`/`hashCode`); do not add your own.

## 2. Repository

```java
@Repository
public interface ProductRepository extends BaseRepository<Product> {}
```

## 3. Request, DTO and mapper

```java
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductRequest extends BaseRequest {
  @NotBlank private String name;
  @NotNull @Positive private BigDecimal price;
}

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ProductDto extends BaseDto {
  private String name;
  private BigDecimal price;
}

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper extends BaseMapper<ProductRequest, Product, ProductDto> {}
```

Put validation on the request: the generic endpoints validate it with `@Valid`.

## 4. Service

```java
@Service
@Transactional
public class ProductService
    extends AbstractService<ProductRequest, Product, ProductDto, ProductRepository, ProductMapper> {

  public ProductService(ProductMapper mapper, ProductRepository repository) {
    super(mapper, repository);
  }

  @Override
  protected Set<String> filterableFields() {
    return Set.of("name", "price", "category", "createdAt");
  }
}
```

Only the listed fields can be filtered (see [Filtering](#filtering)). Never list sensitive fields.
Business rules go into the hooks (`beforeCreate`, `afterUpdate`, ...) or into extra methods.

## 5. Controller

```java
@RestController
@RequestMapping("/api/v1/product")
@Tag(name = "10. Products", description = "APIs for managing products")
@RequirePermission(page = "PRODUCTS")
public class ProductRestApi extends AbstractCrudRestApi<ProductRequest, ProductDto> {

  public ProductRestApi(ProductService service) {
    super(service);
  }
}
```

This exposes, with the permissions they require:

| Endpoint | Permission |
| --- | --- |
| `GET /api/v1/product` (paged, filters as query parameters) | `PRODUCTS:VIEW` |
| `GET /api/v1/product/list` (unpaged) | `PRODUCTS:VIEW` |
| `GET /api/v1/product/{id}` | `PRODUCTS:VIEW` |
| `GET /api/v1/product/{id}/exists` | `PRODUCTS:VIEW` |
| `GET /api/v1/product/count` | `PRODUCTS:VIEW` |
| `POST /api/v1/product/search` (paged, filters in the body) | `PRODUCTS:VIEW` |
| `POST /api/v1/product` | `PRODUCTS:CREATE` |
| `PUT /api/v1/product/{id}` | `PRODUCTS:EDIT` |
| `DELETE /api/v1/product/{id}` (soft delete) | `PRODUCTS:DELETE` |
| `POST /api/v1/product/{id}/restore` | `PRODUCTS:DELETE` |

Add resource-specific endpoints as ordinary methods of the controller (see `RoleRestApi`).

## 6. Permission catalog

Declare the page in a `PermissionCatalogContributor` (see `docs/PERMISSION_MANAGEMENT.md`):

```java
catalog.page("PRODUCTS").name("Products").url("/products").icon("fa-solid fa-box")
    .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
```

On the next start the page and its actions are created and granted to `ROLE_ADMIN`. If the
controller requires a page or action the catalog does not declare, the application does not start.

## Filtering

List endpoints accept filters for the fields the service declares filterable. Other parameters,
including `page`, `size` and `sort`, are ignored by the filter.

| Parameter | Meaning |
| --- | --- |
| `name=lap` | text field contains the value, case-insensitive |
| `price=100` | equality for numbers, booleans, enums (case-insensitive) and ids |
| `createdAt=2026-10-16` | a date on a date-time field matches the whole day |
| `priceFrom=100` / `priceTo=500` | inclusive range; `createdAtTo=2026-10-16` includes that day |
| `categoryId=7` | many-to-one relation by id |
| `deleted=true` | only soft deleted records (they are hidden otherwise) |

A value that does not fit its field (e.g. `price=abc`) is answered with 400.
