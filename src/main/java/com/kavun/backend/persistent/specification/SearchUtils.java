package com.kavun.backend.persistent.specification;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Folds text for search, so that a filter matches regardless of case, accents and how the text was
 * typed, in any language: {@code İSTANBUL}, {@code Şükrü}, {@code Straße}, {@code ΟΔΟΣ} and
 * {@code Ｋａｖｕｎ} fold to {@code istanbul}, {@code sukru}, {@code strasse}, {@code οδοσ} and
 * {@code kavun}.
 *
 * <p>The database folds the stored values with the {@code search_fold} SQL function (changeset
 * {@code 003-search-fold.xml}): on PostgreSQL the same steps in SQL, on H2 an alias of {@link
 * #fold(String)}. A change here needs the same change there, in a new changeset.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class SearchUtils {

  /**
   * Accents of Latin, Greek and Cyrillic letters, Hebrew points and Arabic harakat. Marks that
   * spell a word in other scripts (Thai and Indic vowels, Japanese voicing) are kept.
   */
  private static final Pattern MARKS =
      Pattern.compile("[\\u0300-\\u036F\\u0591-\\u05C7\\u064B-\\u065F\\u0670]");

  private SearchUtils() {}

  /**
   * Folds a text for search.
   *
   * @param value the text
   * @return the folded text, or null for null
   */
  public static String fold(String value) {
    if (value == null) {
      return null;
    }
    // Compatibility decomposition splits accents off their letters and turns full-width forms and
    // ligatures into plain letters; NFC then recomposes what is left (Hangul, kana).
    String text = Normalizer.normalize(value, Normalizer.Form.NFKD);
    text = MARKS.matcher(text).replaceAll("");
    text = Normalizer.normalize(text, Normalizer.Form.NFC);
    // Upper then lower case, locale-independently: ß becomes ss, the Turkish ı becomes i.
    text = text.toUpperCase(Locale.ROOT).toLowerCase(Locale.ROOT);
    // The final sigma, and letters that have no decomposition to drop an accent from.
    return StringUtils.replaceChars(text, "ςłøđħ", "σlodh");
  }
}
