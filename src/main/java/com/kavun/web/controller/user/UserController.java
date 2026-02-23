package com.kavun.web.controller.user;

import com.kavun.annotation.Loggable;
import com.kavun.backend.persistent.domain.user.User;
import com.kavun.backend.service.security.AuditService;
import com.kavun.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * This controller handles all requests from the browser relating to user profile.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
@PreAuthorize("isAuthenticated() and hasAnyRole(T(com.kavun.enums.RoleType).ROLE_ADMIN)")
public class UserController {

  private final UserService userService;
  private final AuditService auditService;

  /** View user's page. */
  @Loggable
  @GetMapping
  public String users() {
    return "user/index";
  }

  /**
   * Retrieves the users in the application.
   *
   * @return list of users page

  @Loggable
  @PostMapping("/datatables")
  public @ResponseBody DataTablesOutput<UserResponse> getUsers(
      @RequestBody @Valid DataTablesInput input) {
    return userService.getUsers(input);
  }*/

  @GetMapping("/audits")
  public String auditHistory(Model model) {
    var auditLogs = auditService.getAuditLogs(User.class, true, true, true);

    model.addAttribute("auditLogs", auditLogs);
    return "user/index";
  }
}
