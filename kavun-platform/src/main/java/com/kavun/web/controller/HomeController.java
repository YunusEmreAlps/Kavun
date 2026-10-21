package com.kavun.web.controller;

import com.kavun.constant.HomeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HomeController class manages all incoming request to the home page.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Controller
@RequiredArgsConstructor
@RequestMapping(HomeConstants.INDEX_URL_MAPPING)
public class HomeController {

  /**
   * Maps index url request.
   *
   * @return index view name
   */
  @GetMapping
  public String home() {
    return HomeConstants.INDEX_VIEW_NAME;
  }
}
