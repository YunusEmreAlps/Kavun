package com.kavun.backend.service.security.permission;

import com.kavun.enums.ActionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * The pages and actions the application knows about, declared in code by {@link
 * PermissionCatalogContributor}s. It is the contract endpoints are checked against at startup and
 * the seed that is synchronized into the database.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class PermissionCatalog {

  private final Map<String, ActionDefinition> actions;
  private final Map<String, PageDefinition> pages;

  private PermissionCatalog(
      Map<String, ActionDefinition> actions, Map<String, PageDefinition> pages) {
    this.actions = Collections.unmodifiableMap(actions);
    this.pages = Collections.unmodifiableMap(pages);
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Actions in declaration order. */
  public Collection<ActionDefinition> getActions() {
    return actions.values();
  }

  /** Pages ordered so that every parent comes before its children. */
  public Collection<PageDefinition> getPages() {
    return pages.values();
  }

  /**
   * Whether the page may only be used by the platform tenant.
   *
   * @param pageCode the page code
   * @return true for platform-only pages
   */
  public boolean isPlatformOnly(String pageCode) {
    var page = pages.get(pageCode);
    return page != null && page.platformOnly();
  }

  /**
   * Whether the page declares the action.
   *
   * @param code the page action code
   * @return true if the catalog contains it
   */
  public boolean contains(PageActionCode code) {
    var page = pages.get(code.page());
    return page != null && page.actionCodes().contains(code.action());
  }

  /**
   * An action that can be performed on pages.
   *
   * @param code the unique action code
   * @param name the display name, also used as the default label of its page actions
   * @param type how the action is presented
   */
  public record ActionDefinition(String code, String name, ActionType type) {}

  /**
   * A page (menu entry) and the actions it supports.
   *
   * @param code the unique page code
   * @param name the display name
   * @param url the frontend route
   * @param icon the menu icon
   * @param description an optional description
   * @param parentCode the parent page code, null for a root page
   * @param showInMenu whether the page is listed in the navigation menu
   * @param navigable whether the page can be navigated to
   * @param order the position among its siblings
   * @param platformOnly whether only the platform tenant may use the page
   * @param actionCodes the actions the page supports
   */
  public record PageDefinition(
      String code,
      String name,
      String url,
      String icon,
      String description,
      String parentCode,
      boolean showInMenu,
      boolean navigable,
      int order,
      boolean platformOnly,
      List<String> actionCodes) {}

  /** Collects the definitions of all contributors and validates them as a whole. */
  public static final class Builder {

    private final Map<String, ActionDefinition> actions = new LinkedHashMap<>();
    private final Map<String, PageSpec> pages = new LinkedHashMap<>();

    private Builder() {}

    /**
     * Declares an action.
     *
     * @param code the unique action code
     * @param name the display name
     * @param type how the action is presented
     * @return this builder
     */
    public Builder action(String code, String name, ActionType type) {
      require(StringUtils.isNotBlank(code), "Action code must not be blank");
      require(StringUtils.isNotBlank(name), "Action " + code + " needs a name");
      require(
          actions.putIfAbsent(code, new ActionDefinition(code, name, Objects.requireNonNull(type)))
              == null,
          "Action " + code + " is declared more than once");
      return this;
    }

    /**
     * Declares a page; configure it through the returned spec.
     *
     * @param code the unique page code
     * @return the page spec
     */
    public PageSpec page(String code) {
      require(StringUtils.isNotBlank(code), "Page code must not be blank");
      var spec = new PageSpec(code, pages.size());
      require(pages.putIfAbsent(code, spec) == null, "Page " + code + " is declared more than once");
      return spec;
    }

    /**
     * Validates the declarations and orders pages parents first.
     *
     * @return the catalog
     * @throws IllegalStateException if the declarations are inconsistent
     */
    public PermissionCatalog build() {
      for (PageSpec page : pages.values()) {
        require(StringUtils.isNotBlank(page.name), "Page " + page.code + " needs a name");
        require(!page.actionCodes.isEmpty(), "Page " + page.code + " declares no actions");
        for (String action : page.actionCodes) {
          require(
              actions.containsKey(action),
              "Page " + page.code + " uses undeclared action " + action);
        }
        require(
            page.parentCode == null || pages.containsKey(page.parentCode),
            "Page " + page.code + " has undeclared parent " + page.parentCode);
      }

      Map<String, PageDefinition> ordered = new LinkedHashMap<>();
      while (ordered.size() < pages.size()) {
        int before = ordered.size();
        for (PageSpec page : pages.values()) {
          if (!ordered.containsKey(page.code)
              && (page.parentCode == null || ordered.containsKey(page.parentCode))) {
            ordered.put(page.code, page.toDefinition());
          }
        }
        require(ordered.size() > before, "Page hierarchy contains a cycle");
      }
      return new PermissionCatalog(new LinkedHashMap<>(actions), ordered);
    }

    private static void require(boolean condition, String message) {
      if (!condition) {
        throw new IllegalStateException("Invalid permission catalog: " + message);
      }
    }
  }

  /** Fluent declaration of a page; unset values get sensible defaults. */
  public static final class PageSpec {

    private static final String DEFAULT_ICON = "fa-solid fa-folder";
    private static final int ORDER_STEP = 10;

    private final String code;
    private final int position;
    private String name;
    private String url;
    private String icon = DEFAULT_ICON;
    private String description;
    private String parentCode;
    private boolean showInMenu = true;
    private boolean navigable = true;
    private boolean platformOnly;
    private final List<String> actionCodes = new ArrayList<>();

    private PageSpec(String code, int position) {
      this.code = code;
      this.position = position;
    }

    public PageSpec name(String name) {
      this.name = name;
      return this;
    }

    /** The frontend route; defaults to the page code in kebab case, e.g. /page-actions. */
    public PageSpec url(String url) {
      this.url = url;
      return this;
    }

    public PageSpec icon(String icon) {
      this.icon = icon;
      return this;
    }

    public PageSpec description(String description) {
      this.description = description;
      return this;
    }

    public PageSpec parent(String parentCode) {
      this.parentCode = parentCode;
      return this;
    }

    public PageSpec hiddenFromMenu() {
      this.showInMenu = false;
      return this;
    }

    /** For grouping pages that only exist to hold children in the menu. */
    public PageSpec notNavigable() {
      this.navigable = false;
      return this;
    }

    /**
     * For pages that act on all tenants (e.g. tenant management, the shared permission catalog):
     * never granted to, nor usable by, other tenants, even their administrators.
     */
    public PageSpec platformOnly() {
      this.platformOnly = true;
      return this;
    }

    public PageSpec actions(String... actionCodes) {
      this.actionCodes.addAll(List.of(actionCodes));
      return this;
    }

    private PageDefinition toDefinition() {
      String route =
          StringUtils.isNotBlank(url)
              ? url
              : "/" + code.toLowerCase(Locale.ROOT).replace('_', '-');
      return new PageDefinition(
          code,
          name,
          route,
          icon,
          description,
          parentCode,
          showInMenu,
          navigable,
          (position + 1) * ORDER_STEP,
          platformOnly,
          List.copyOf(actionCodes));
    }
  }
}
