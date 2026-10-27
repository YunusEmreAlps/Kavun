package com.kavun.backend.persistent.specification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SearchUtilsTest {

  @ParameterizedTest(name = "{0} finds {1}")
  @CsvSource({
    "istanbul, İSTANBUL",
    "İstanbul, istanbul",
    "ışık, IŞIK",
    "IZMIR, kuyumcu izmir subesi",
    "sukru, Şükrü",
    "strasse, Straße",
    "kavun, Ｋａｖｕｎ",
  })
  void findsTextRegardlessOfCaseAndAccents(String filter, String text) {
    Assertions.assertTrue(SearchUtils.fold(text).contains(SearchUtils.fold(filter)));
  }

  @Test
  void foldsDecomposedText() {
    // "cafe" followed by a combining acute accent.
    Assertions.assertEquals("cafe", SearchUtils.fold("cafe\u0301"));
  }

  @Test
  void dropsArabicHarakat() {
    // Muhammad with and without harakat.
    Assertions.assertEquals(
        "\u0645\u062D\u0645\u062F",
        SearchUtils.fold("\u0645\u064F\u062D\u064E\u0645\u0651\u064E\u062F"));
  }

  /** Hindi, Thai, Korean and Japanese (ga), whose marks are part of the letters. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "\u0939\u093F\u0902\u0926\u0940",
        "\u0E20\u0E32\u0E29\u0E32\u0E44\u0E17\u0E22",
        "\uD55C\uAD6D\uC5B4",
        "\u30AC"
      })
  void keepsMarksThatSpellTheWord(String text) {
    Assertions.assertEquals(text, SearchUtils.fold(text));
  }

  @Test
  void keepsNull() {
    Assertions.assertNull(SearchUtils.fold(null));
  }
}
