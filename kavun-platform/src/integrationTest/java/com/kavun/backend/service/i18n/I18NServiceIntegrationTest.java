package com.kavun.backend.service.i18n;

import com.kavun.IntegrationTestUtils;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

class I18NServiceIntegrationTest extends IntegrationTestUtils {

  private static final String MESSAGE_KEY = "error.access.denied";

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
    LocaleContextHolder.setDefaultLocale(null);
  }

  @Test
  void getMessage() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    Assertions.assertEquals(
        "You do not have permission to perform this action.", i18NService.getMessage(MESSAGE_KEY));
  }

  @Test
  void getMessageWithTurkishLocale() {
    LocaleContextHolder.setLocale(Locale.forLanguageTag("tr-TR"));

    Assertions.assertEquals(
        "Bu işlemi gerçekleştirmek için izniniz bulunmamaktadır.",
        i18NService.getMessage(MESSAGE_KEY));
  }

  @Test
  void getMessageWithUnsupportedLocaleFallsBackToDefault() {
    LocaleContextHolder.setLocale(Locale.FRENCH);

    Assertions.assertEquals(
        "You do not have permission to perform this action.", i18NService.getMessage(MESSAGE_KEY));
  }
}
