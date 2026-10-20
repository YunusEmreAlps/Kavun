package com.kavun.web.payload.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The page actions a role is granted, as a matrix: one row per page (in menu order, {@code level}
 * is the depth in the page tree) and one cell per page action of that page.
 *
 * @param role the role
 * @param editable whether the caller may change the role's grants
 * @param actions the matrix columns: every action used by a cell, in catalog order
 * @param pages the matrix rows
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public record RolePermissionMatrixResponse(
    RoleSummary role, boolean editable, List<ActionColumn> actions, List<PageRow> pages) {

  /** The role the matrix belongs to. */
  public record RoleSummary(Long id, String name, String label) {}

  /** A matrix column. */
  public record ActionColumn(String code, String name) {}

  /** A matrix row; a page without page actions of its own only groups its children. */
  public record PageRow(Long id, String code, String name, int level, List<Cell> cells) {}

  /**
   * A page action of a page.
   *
   * @param actionCode the column of the cell
   * @param pageActionId the page action, sent back when saving the matrix
   * @param state the role's current grant
   * @param expiresAt when a grant or denial expires, if it does
   * @param grantable whether the caller may grant the page action (it holds it, or is an admin)
   */
  public record Cell(
      String actionCode,
      Long pageActionId,
      State state,
      LocalDateTime expiresAt,
      boolean grantable) {}

  /**
   * The role's grant of a page action. A denial (granted=false), set through the permission API,
   * overrides grants from the user's other roles; the matrix leaves it alone unless the cell is
   * checked.
   */
  public enum State {
    GRANTED,
    DENIED,
    NONE
  }
}
