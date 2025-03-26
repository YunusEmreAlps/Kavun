package com.kavun.web.controller;

import com.kavun.constant.HomeConstants;
import com.kavun.constant.SecurityConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.shared.util.core.SecurityUtils;
import com.kavun.web.payload.request.LoginRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
   * The login mapping.
   *
   * @param model the model
   * @return the login page.
   */
  @GetMapping(path = SecurityConstants.LOGIN)
  public String login(Model model) {

    // if the user is authenticated, redirect to the home page.
    if (SecurityUtils.isAuthenticated()) {
      return HomeConstants.REDIRECT_TO_INDEX;
    }
    model.addAttribute(UserConstants.USER_MODEL_KEY, new LoginRequest());

    return SecurityConstants.LOGIN_VIEW_NAME;
  }
}
