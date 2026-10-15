package com.kavun.web.rest.v1;

import com.kavun.annotation.RequirePermission;
import com.kavun.backend.service.user.PageService;
import com.kavun.constant.PermissionConstants.Pages;
import com.kavun.shared.dto.PageDto;
import com.kavun.shared.request.PageRequest;
import com.kavun.web.rest.AbstractCrudRestApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD endpoints for pages.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/page")
@Tag(name = "05. Page Management", description = "APIs for managing pages")
@RequirePermission(page = Pages.PAGES)
public class PageRestApi extends AbstractCrudRestApi<PageRequest, PageDto> {

  public PageRestApi(PageService service) {
    super(service);
  }
}
