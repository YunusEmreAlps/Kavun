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
    private boolean access;

    @Builder.Default
    private List<ActionResponse> actions = new ArrayList<>();

    @Builder.Default
    private List<NavigationItemResponse> children = new ArrayList<>();
}
