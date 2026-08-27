package com.kavun.web.controller;

import com.kavun.constant.HomeConstants;
import com.kavun.constant.SecurityConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The controller for handling all security-related mappings.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Controller
public class SecurityController {

  /**
   * The login mapping. Authentication now happens through the REST API, so this page only exists
   * to satisfy Spring Security's {@code loginPage} requirement and sends visitors to the home
   * page.
   *
   * @return redirect to the home page.
   */
  @GetMapping(path = SecurityConstants.LOGIN)
  public String login() {
    return HomeConstants.REDIRECT_TO_INDEX;
  }
}
