package com.kavun.web.controller.user;

import com.kavun.annotation.Loggable;
import com.kavun.backend.service.user.UserService;
import com.kavun.constant.ErrorConstants;
import com.kavun.constant.HomeConstants;
import com.kavun.constant.user.ProfileConstants;
import com.kavun.constant.user.UserConstants;
import com.kavun.enums.UserHistoryType;
import com.kavun.shared.dto.UserDto;
import com.kavun.shared.dto.UserHistoryDto;
import com.kavun.shared.util.UserUtils;
import com.kavun.shared.util.core.SecurityUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
@RepositoryRestResource(exported = false)
@RequestMapping(ProfileConstants.PROFILE_MAPPING)
@PreAuthorize("isAuthenticated() and hasAnyRole(T(com.kavun.enums.RoleType).values())")
public class UserProfileController {

  private final UserService userService;

  /**
   * View user's page.
   *
   * @param model The model to convey objects to view layer
   * @param redirectAttributes the redirectAttribute
   * @return profile page.
   */
  @Loggable
  @GetMapping
  public String profile(Principal principal, Model model, RedirectAttributes redirectAttributes) {

    var userDto = userService.findByUsername(principal.getName());
    if (Objects.isNull(userDto)) {
      redirectAttributes.addFlashAttribute(ErrorConstants.ERROR, true);
      return HomeConstants.REDIRECT_TO_LOGIN;
    }

    var userHistoryDtos = UserUtils.convertToUserHistoryDto(userDto.getUserHistories());
    userHistoryDtos.sort(Comparator.comparing(UserHistoryDto::getCreatedAt).reversed());

    model.addAttribute(ProfileConstants.USER_HISTORIES, userHistoryDtos);

    // set default to true if no new account or update is requested.
    model.addAttribute(ProfileConstants.DEFAULT, isDefaultOrUpdateMode(model));
    model.addAttribute(UserConstants.USER_MODEL_KEY, userDto);

    return ProfileConstants.USER_PROFILE_VIEW_NAME;
  }

  /**
   * Updates the user profile with the details provided.
   *
   * @param user the user
   * @param result the binding result
   * @param model the model with redirection
   * @return the view to profile page.
   */
  @Loggable
  @PreAuthorize("isFullyAuthenticated()")
  @PostMapping(ProfileConstants.PROFILE_UPDATE_MAPPING)
  public String updateProfile(
      @Valid @ModelAttribute UserDto user, BindingResult result, RedirectAttributes model) {

    var userDetails = SecurityUtils.getAuthenticatedUserDetails();
    if (result.hasErrors()
        || Objects.isNull(userDetails)
        || !user.getEmail().equals(userDetails.getEmail())) {
      model.addFlashAttribute(ErrorConstants.ERROR, true);
      return HomeConstants.REDIRECT_TO_LOGIN;
    }

    UserUtils.enableUser(user);
    user.setPublicId(userDetails.getPublicId());
    userService.updateUser(user, UserHistoryType.PROFILE_UPDATE);

    // Authenticate user with the updated profile.
    SecurityUtils.authenticateUser(userDetails);

    return ProfileConstants.REDIRECT_TO_PROFILE;
  }

  /**
   * returns true if no new account or update is requested.
   *
   * @param model the model
   */
  private boolean isDefaultOrUpdateMode(Model model) {
    return !model.containsAttribute(ProfileConstants.NEW_PROFILE)
        && !model.containsAttribute(ProfileConstants.UPDATE_MODE);
  }
}
