package com.kavun.web.controller;

import com.kavun.constant.HomeConstants;
import com.kavun.constant.SecurityConstants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SecurityControllerTest {

  private transient MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.standaloneSetup(SecurityController.class).build();
  }

  @Test
  void testLoginPathRedirectsToHome() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(SecurityConstants.LOGIN))
        .andExpect(MockMvcResultMatchers.redirectedUrl(HomeConstants.INDEX_URL_MAPPING))
        .andExpect(MockMvcResultMatchers.status().isFound());
  }
}
