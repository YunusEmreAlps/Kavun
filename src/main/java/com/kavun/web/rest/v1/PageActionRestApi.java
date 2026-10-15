package com.kavun.web.rest.v1;

import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.user.PageActionService;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.PageActionDto;
import com.kavun.shared.request.PageActionRequest;
import com.kavun.web.rest.AbstractCrudRestApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD endpoints for page actions.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/page-action")
@Tag(name = "06. Page Action Management", description = "APIs for managing page actions")
@RequirePermission(page = Pages.PAGE_ACTIONS)
public class PageActionRestApi extends AbstractCrudRestApi<PageActionRequest, PageActionDto> {

  public PageActionRestApi(PageActionService service) {
    super(service);
  }
}
