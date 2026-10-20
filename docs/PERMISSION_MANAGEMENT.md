# Permission Management

Every API endpoint declares who may call it. The declaration lives on the controller, is checked by
the server, and is verified when the application starts. Nothing the client sends (headers, URLs)
decides which permission applies.

## Table of Contents

- [Concepts](#concepts)
- [Declaring access on endpoints](#declaring-access-on-endpoints)
- [The permission catalog](#the-permission-catalog)
- [How a request is authorized](#how-a-request-is-authorized)
- [Escalation guards](#escalation-guards)
- [Granting permissions to a role](#granting-permissions-to-a-role)
- [Tokens and sessions](#tokens-and-sessions)
- [Adding a new module](#adding-a-new-module)
- [Frontend integration](#frontend-integration)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

## Concepts

A permission is an action on a page, written `PAGE_CODE:ACTION_CODE`, e.g. `USERS:EDIT`.

- **Page** (`WebPage`): an area of the application and a navigation menu entry (`USERS`, `ROLES`).
- **Action** (`Action`): something that can be done on pages (`VIEW`, `CREATE`, `EDIT`, `DELETE`,
  `ASSIGN`).
- **Page action** (`PageAction`): an action a page supports; this is what gets granted.
- **Permission** (`Permission`): a grant (or an explicit deny) of a page action to a role or to a
  single user, optionally with an expiry date.

Permissions are resolved in this order; the first level that has a record decides:

1. User level: any non-expired deny wins, otherwise any non-expired grant allows.
2. Role level (all roles of the user): any non-expired deny wins, otherwise any grant allows.
3. Nothing found: access is denied.

## Declaring access on endpoints

Every endpoint under `/api` must carry exactly one of these annotations, on the method or on its
controller. A method annotation replaces the controller's.

| Annotation | Who may call |
| --- | --- |
| `@PublicEndpoint` | anyone, without authentication (login, password reset, ...) |
| `@AuthenticatedEndpoint` | any signed-in user; only for endpoints acting on the caller's own data |
| `@RequirePermission` | signed-in users holding the permission |

### `@RequirePermission`

Put the page on the controller; the action is implied by the HTTP method:

| HTTP method | Action |
| --- | --- |
| GET, HEAD | `VIEW` |
| POST | `CREATE` |
| PUT, PATCH | `EDIT` |
| DELETE | `DELETE` |

Override the action where the verb does not match the business action:

```java
@RestController
@RequestMapping("/api/v1/role")
@RequirePermission(page = Pages.ROLES)        // GET -> ROLES:VIEW, POST -> ROLES:CREATE, ...
public class RoleRestApi {

  @PostMapping("/search")
  @RequirePermission(action = Actions.VIEW)   // read-only POST
  public Page<RoleDto> search(...) { ... }

  @PostMapping("/{id}/restore")
  @RequirePermission(action = Actions.DELETE) // restoring undoes a delete
  public RoleDto restore(...) { ... }

  @PostMapping("/assign-roles/{userId}")
  @RequirePermission(action = Actions.ASSIGN)
  public void assignRoles(...) { ... }
}
```

A method-level `@RequirePermission` uses the controller's page when it does not set one, and may
also name another page (`@RequirePermission(page = Pages.INVOICES)`). `action` belongs on methods
only; the controller-level annotation takes a page.

Use the constants in `PermissionConstants.Pages` / `PermissionConstants.Actions` (or your module's
own constants) rather than string literals.

### Startup validation

`EndpointPolicyValidator` checks every `/api` endpoint when the application starts and refuses to
start if an endpoint:

- has no access annotation,
- has more than one on the same element,
- uses `@RequirePermission` without a page, with an `action` on the controller, or with an HTTP
  method that implies no action,
- requires a page action that the permission catalog does not declare.

A forgotten annotation therefore fails the build (the integration tests start the application)
instead of shipping an endpoint that is open or that nobody can use.

## The permission catalog

Pages and actions are declared in code by `PermissionCatalogContributor` beans. The platform's own
administration pages are in `CorePermissionCatalog`:

```java
@Component
public class CorePermissionCatalog implements PermissionCatalogContributor {

  @Override
  public void contribute(PermissionCatalog.Builder catalog) {
    catalog.action(Actions.VIEW, "View", ActionType.PAGE);
    // ...
    catalog
        .page(Pages.USERS)
        .parent(Pages.ADMINISTRATION)
        .name("Users")
        .url("/administration/users")
        .icon("fa-solid fa-users")
        .actions(Actions.VIEW, Actions.CREATE, Actions.EDIT, Actions.DELETE);
  }
}
```

The catalog is validated as a whole (unique codes, existing parents and actions, no cycles); an
invalid catalog stops the application.

On every startup, after the roles are seeded, `PermissionCatalogSynchronizer` writes the catalog to
the database:

- Missing actions, pages and page actions are created. Existing rows are left as they are, so
  names, icons or menu order changed by administrators are kept, and rows they soft deleted are not
  brought back.
- Every newly created page action is granted to the `ROLE_ADMIN` of every tenant, so
  administrators can use a new feature right away. If an administrator later revokes such a grant,
  it stays revoked. A newly provisioned tenant gets all current page actions for its `ROLE_ADMIN`.
- Pages marked `.platformOnly()` (tenant management, the shared catalog pages) are granted to the
  platform tenant's `ROLE_ADMIN` only and refused to every other tenant; see
  `docs/MULTI_TENANCY.md`.

## How a request is authorized

The Spring Security filter chain only authenticates (`JwtAuthTokenFilter`). Authorization happens in
`EndpointAuthorizationInterceptor`, a Spring MVC interceptor on `/api/**` that runs once the
controller method is known but before the request body is read:

1. It resolves the method's policy (`EndpointPolicyResolver`).
2. `@PublicEndpoint` passes, `@AuthenticatedEndpoint` requires a valid token, and
   `@RequirePermission` additionally asks `AccessControlService` whether the caller holds the
   permission.

Requests that match no endpoint get 404. Denials go through the global exception handler and use
the standard envelope:

- **401** `UNAUTHORIZED`: no valid token.
- **403** `FORBIDDEN`: valid token, missing permission. The message is localized from the
  `Accept-Language` header (`error.access.denied` in `i18n/messages*.properties`).

For checks inside business logic, call `AccessControlService.checkPermission(page, action)`; it
throws `AccessDeniedException`, which the global handler turns into the same 403 response.

## Escalation guards

Holding a permission must not let a user grant themselves more. These rules are enforced in the
services, whichever endpoint is used:

- Assigning roles requires `ROLES:ASSIGN`, also when roles are sent while creating or updating a
  user. Re-sending the roles a user already has needs no extra permission.
- Only administrators can assign `ROLE_ADMIN`.
- Only administrators can change, enable/disable, delete or re-role administrator accounts (a user
  manager could otherwise take an admin account over by changing its email).
- Only administrators can change, delete or restore the built-in roles (`ROLE_ADMIN`, `ROLE_USER`).
- Only page actions the caller holds itself can be granted (administrators can grant any), in the
  permission matrix and through the Permission API alike: a permission manager cannot hand out, or
  give itself through a role it holds, more than it has.
- In the permission matrix, the grants of `ROLE_ADMIN` cannot be changed (the catalog keeps them
  complete, so administrators cannot lock themselves out) and those of the other built-in roles only
  by administrators.

## Granting permissions to a role

The permission matrix of a role shows every page the tenant may use (platform-only pages only to the
platform) with a cell per page action, and saves the checked cells in one request:

```http
GET /api/v1/permission/role/{roleId}         # PERMISSIONS:VIEW
PUT /api/v1/permission/role/{roleId}         # PERMISSIONS:EDIT
{ "pageActionIds": [19, 20, 31] }
```

The response lists the action columns and the pages in menu order (`level` is the depth in the page
tree). Each cell has its `pageActionId`, the role's `state` (`GRANTED`, `DENIED` or `NONE`; expired
records count as `NONE`), `expiresAt`, and `grantable` (whether the caller may grant it). Saving only
writes the cells whose state changes:

- a checked cell that is not granted is granted, reusing the role's existing record of the page
  action (a soft-deleted, expired or denying one) because records are unique per role and page
  action;
- an unchecked granted cell is soft deleted;
- a denial stays until its cell is checked.

User-level permissions, denials and expiry dates are set through the Permission API
(`/api/v1/permission`).

## Tokens and sessions

An access token is accepted only from the `Authorization: Bearer` header (never from a cookie: CSRF
protection is off for the stateless API) and only while:

- the user can still sign in (enabled, not locked, not expired, not deleted), and
- the session the token was issued for is active.

Disabling a user or logging out therefore takes effect on the next request instead of when the
token expires. A refresh continues a session that only timed out; a session that was logged out
(manually or by force) cannot be refreshed and the user has to sign in again.

## Adding a new module

1. Add page/action codes as constants of the module.
2. Register a `PermissionCatalogContributor` that declares the module's pages (and any new actions).
3. Annotate the module's controllers with `@RequirePermission(page = ...)`; mark read-only POSTs and
   other non-standard actions explicitly.
4. Start the application (or run the integration tests): the validator lists anything missing, and
   the synchronizer creates the pages and grants them to `ROLE_ADMIN`.
5. Grant the new page actions to other roles in their permission matrix.

## Frontend integration

- Build the menu from `GET /api/v1/navigation`: it returns the pages the user may view, with the
  other actions the user may perform on each page, so buttons can be shown or hidden.
- A page the user may not view still appears when one of its children is accessible, as a heading
  with `access: false` and no actions: render it without a link. Grouping pages
  (`navigable: false`) therefore need no permission of their own; they are listed when they contain
  an accessible page and left out when they are empty. Granting `USERS:VIEW` alone is enough for
  "Administration > Users" to appear.
- No `Page-Code` or `Page-Url` header is needed; they are ignored.
- Send `Accept-Language` (`en`, `tr`) to get localized error messages.

## Configuration

```properties
# ROLE_ADMIN passes every permission check without explicit grants.
# On in development, off in production (administrators rely on the catalog grants there).
security.permission.admin-bypass-enabled=false
```

## Troubleshooting

**The application does not start and lists endpoints.** Each line names the endpoint and the
problem: add the missing annotation, set the action for non-standard HTTP methods, or declare the
page/action in a catalog contributor.

**A user gets 403 unexpectedly.** Check that the page action exists and is not soft deleted, that
the user's role has a non-expired grant for it, and that there is no user-level or role-level deny.

**An administrator gets 403 in production.** The grant of that page action to `ROLE_ADMIN` was
revoked or the page action was created manually (only page actions created by the synchronizer are
granted automatically). Grant it through the Permission API.
