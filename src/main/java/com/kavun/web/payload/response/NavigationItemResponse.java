package com.kavun.web.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigation item response for API.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemResponse {

    private Long id;
    private String code;
    private String label;
    private String url;
    private String icon;
    private int level;

    /**
     * Whether the user may open the page. False for a heading listed only because one of its
     * children is accessible; render it without a link.
     */
    private boolean access;
    private boolean navigable;
    private boolean showInMenu;

    @Builder.Default
    private List<ActionResponse> actions = new ArrayList<>();

    @Builder.Default
    private List<NavigationItemResponse> children = new ArrayList<>();
}
