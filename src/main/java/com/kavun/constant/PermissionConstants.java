package com.kavun.constant;

/**
 * Page and action codes of the permission catalog. Endpoints reference them through
 * {@link com.kavun.annotation.RequirePermission}, so a typo is a compile error rather than an
 * endpoint nobody can call.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class PermissionConstants {

  private PermissionConstants() {
    throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
  }

  /** Action codes. VIEW, CREATE, EDIT and DELETE are implied by GET, POST, PUT/PATCH, DELETE. */
  public static final class Actions {
    public static final String VIEW = "VIEW";
    public static final String CREATE = "CREATE";
    public static final String EDIT = "EDIT";
    public static final String DELETE = "DELETE";
    public static final String ASSIGN = "ASSIGN";

    private Actions() {
      throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
    }
  }

  /** Page codes of the platform administration area. */
  public static final class Pages {
    public static final String ADMINISTRATION = "ADMINISTRATION";
    public static final String USERS = "USERS";
    public static final String USER_REPORTS = "USER_REPORTS";
    public static final String ROLES = "ROLES";
    public static final String PERMISSIONS = "PERMISSIONS";
    public static final String PAGES = "PAGES";
    public static final String ACTIONS = "ACTIONS";
    public static final String PAGE_ACTIONS = "PAGE_ACTIONS";

    private Pages() {
      throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
    }
  }
}
