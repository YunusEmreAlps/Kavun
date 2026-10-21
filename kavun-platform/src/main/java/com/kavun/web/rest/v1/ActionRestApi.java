package com.kavun.web.rest.v1;

import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.user.ActionService;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.ActionDto;
import com.kavun.shared.request.ActionRequest;
import com.kavun.web.rest.AbstractCrudRestApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD endpoints for actions.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/action")
@Tag(name = "04. Action Management", description = "APIs for managing actions")
@RequirePermission(page = Pages.ACTIONS)
public class ActionRestApi extends AbstractCrudRestApi<ActionRequest, ActionDto> {

  public ActionRestApi(ActionService service) {
    super(service);
  }
}
