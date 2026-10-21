package com.kavun.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

/**
 * Single entry point for masking sensitive data (passwords, tokens, card numbers, national IDs,
 * ...) before it is logged or persisted. Use it whenever something may end up in a log line or an
 * audit column.
 *
 * <ul>
 *   <li>{@link #mask(Object)} - any value: DTOs, maps, collections, JSON strings, or plain text in
 *       {@code key=value} form (Lombok {@code toString()}, query strings).
 *   <li>{@link #maskArguments(String[], Object[])} - method arguments, keyed by parameter name, so
 *       a bare value like a {@code refreshToken} cookie is masked by its name.
 *   <li>{@link #isSensitiveKey(String)} - for code that builds its own key/value output.
 * </ul>
 *
 * <p>Objects are converted to a Jackson tree once and masked in a single walk, instead of
 * cloning them or running one regex per sensitive word. Values that cannot be serialized become a
 * {@code <ClassName>} placeholder; this class never falls back to {@code toString()}, since that
 * is exactly what leaks unmasked secrets.
 *
 * @author Yunus Emre Alpu
 * @version 3.0
 * @since 1.0
 */
public final class MaskPasswordUtils {

  /** Replacement written in place of every sensitive value. */
  public static final String MASK = "******";

  private static final JsonMapper MAPPER = JsonMapper.builder()
      .findAndAddModules()
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .build();

  /**
   * Matched anywhere in the key after removing case and separators, so {@code userPassword},
   * {@code refresh_token}, {@code X-Api-Key} and {@code tcKimlikNo} are all caught.
   */
  private static final Set<String> SENSITIVE_SUBSTRINGS = Set.of(
      "password", "passwd", "passphrase", "secret", "token", "apikey", "privatekey",
      "credential", "authorization", "cookie", "sessionid", "creditcard", "cardnumber",
      "tckimlik", "kimlikno", "nationalid");

  /**
   * Too short to match as substrings ("pin" is in "shipping", "auth" in "author"), so these must
   * be a whole word of the key: {@code pin}, {@code userPin}, {@code OTPCode}, {@code iban_no}.
   */
  private static final Set<String> SENSITIVE_WORDS = Set.of(
      "pwd", "pin", "otp", "totp", "mfa", "cvv", "cvc", "ssn", "iban", "auth", "jwt");

  /** Splits keys on separators and camelCase boundaries ("OTPCode" -> OTP, Code). */
  private static final Pattern WORD_BOUNDARY =
      Pattern.compile("[^A-Za-z0-9]+|(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");

  /** {@code key=value} / {@code key: value} pairs in free text, with optionally quoted keys and values. */
  private static final Pattern KEY_VALUE = Pattern.compile(
      "(\"[^\"]*\"|'[^']*'|[A-Za-z0-9_.\\-]+)(\\s*[:=]\\s*)"
          + "(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|[^,&;})\\]\\s]*)");

  /** Types that are never serialized: streams, servlet objects, uploads and validation results. */
  private static final Set<Class<?>> OPAQUE_TYPES = Set.of(
      ServletRequest.class, ServletResponse.class, HttpSession.class, Errors.class,
      MultipartFile.class, Resource.class, InputStream.class, OutputStream.class,
      Reader.class, Writer.class, byte[].class);

  /** Keys come from request payloads, so the cache is bounded to keep it from growing without limit. */
  private static final int MAX_CACHED_KEYS = 10_000;
  private static final Map<String, Boolean> KEY_CACHE = new ConcurrentHashMap<>();

  private MaskPasswordUtils() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }

  /**
   * Returns a log-safe representation of the given value.
   *
   * <p>Objects, maps and collections are returned as JSON with sensitive values replaced by
   * {@link #MASK}. A JSON string is parsed and masked the same way; any other string has its
   * {@code key=value} pairs masked. An {@link HttpEntity} (e.g. {@code ResponseEntity}) is
   * reduced to its body.
   *
   * @param value anything to be logged, may be {@code null}
   * @return masked representation, never {@code null}
   */
  public static String mask(Object value) {
    JsonNode tree = toMaskedTree(value);
    return tree.isTextual() ? tree.textValue() : write(tree);
  }

  /**
   * Masks method arguments as a JSON object keyed by parameter name. The name matters: a raw
   * {@code String refreshToken} argument carries no key of its own and could not be recognised
   * otherwise.
   *
   * @param names parameter names; when {@code null} or too short, {@code argN} is used
   * @param args  argument values
   * @return masked JSON object, never {@code null}
   */
  public static String maskArguments(String[] names, Object[] args) {
    ObjectNode root = MAPPER.createObjectNode();
    if (args == null) {
      return write(root);
    }
    for (int i = 0; i < args.length; i++) {
      String name = names != null && i < names.length && names[i] != null ? names[i] : "arg" + i;
      root.set(name, isSensitiveKey(name) ? TextNode.valueOf(MASK) : toMaskedTree(args[i]));
    }
    return write(root);
  }

  /**
   * Tells whether a field, parameter, header or JSON key holds sensitive data.
   *
   * @param key the name to check
   * @return {@code true} if values under this name must be masked
   */
  public static boolean isSensitiveKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    Boolean cached = KEY_CACHE.get(key);
    if (cached != null) {
      return cached;
    }
    boolean sensitive = computeSensitive(key);
    if (KEY_CACHE.size() < MAX_CACHED_KEYS) {
      KEY_CACHE.put(key, sensitive);
    }
    return sensitive;
  }

  private static boolean computeSensitive(String key) {
    String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    for (String fragment : SENSITIVE_SUBSTRINGS) {
      if (normalized.contains(fragment)) {
        return true;
      }
    }
    return Arrays.stream(WORD_BOUNDARY.split(key))
        .map(word -> word.toLowerCase(Locale.ROOT))
        .anyMatch(SENSITIVE_WORDS::contains);
  }

  private static JsonNode toMaskedTree(Object value) {
    Object target = value instanceof HttpEntity<?> entity ? entity.getBody() : value;
    if (target == null) {
      return NullNode.getInstance();
    }
    if (target instanceof CharSequence text) {
      return maskText(text.toString());
    }
    if (isOpaque(target)) {
      return TextNode.valueOf(placeholder(target));
    }
    try {
      return maskTree(MAPPER.valueToTree(target));
    } catch (RuntimeException | StackOverflowError e) {
      // Lazy JPA proxies, cyclic graphs, types Jackson cannot handle
      return TextNode.valueOf(placeholder(target));
    }
  }

  private static JsonNode maskText(String text) {
    String trimmed = text.strip();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      try {
        return maskTree(MAPPER.readTree(trimmed));
      } catch (JsonProcessingException e) {
        // Not valid JSON (e.g. Lombok toString output) - fall through to key/value masking
      }
    }
    return TextNode.valueOf(maskKeyValuePairs(text));
  }

  /** Masks the tree in place: values under sensitive keys are replaced whole, objects and arrays included. */
  private static JsonNode maskTree(JsonNode node) {
    if (node instanceof ObjectNode object) {
      for (Map.Entry<String, JsonNode> property : object.properties()) {
        if (isSensitiveKey(property.getKey())) {
          property.setValue(TextNode.valueOf(MASK));
        } else {
          maskTree(property.getValue());
        }
      }
    } else if (node instanceof ArrayNode array) {
      array.forEach(MaskPasswordUtils::maskTree);
    }
    return node;
  }

  private static String maskKeyValuePairs(String text) {
    if (text.isEmpty()) {
      return text;
    }
    Matcher matcher = KEY_VALUE.matcher(text);
    return matcher.replaceAll(match -> {
      String key = match.group(1).replaceAll("^[\"']|[\"']$", "");
      if (!isSensitiveKey(key)) {
        return Matcher.quoteReplacement(match.group());
      }
      String value = match.group(3);
      String masked = value.startsWith("\"") ? "\"" + MASK + "\""
          : value.startsWith("'") ? "'" + MASK + "'"
          : MASK;
      return Matcher.quoteReplacement(match.group(1) + match.group(2) + masked);
    });
  }

  private static boolean isOpaque(Object value) {
    for (Class<?> type : OPAQUE_TYPES) {
      if (type.isInstance(value)) {
        return true;
      }
    }
    return false;
  }

  private static String placeholder(Object value) {
    return "<" + value.getClass().getSimpleName() + ">";
  }

  private static String write(JsonNode tree) {
    try {
      return MAPPER.writeValueAsString(tree);
    } catch (JsonProcessingException e) {
      return "<unserializable>";
    }
  }
}
