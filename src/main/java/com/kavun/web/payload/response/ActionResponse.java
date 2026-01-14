package com.kavun.web.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Action response for API.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionResponse {

    private String code;
    private String label;
    private String type;
}
