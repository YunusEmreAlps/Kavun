package com.kavun.shared.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for masking sensitive information (passwords, tokens, credit
 * cards, etc.) in logs.
 *
 * @author Yunus Emre Alpu
 * @version 2.1
 * @since 1.0
 */
@Slf4j
public final class MaskPasswordUtils {

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    private static final String MASK = "******";
    private static final int MAX_DEPTH = 10; // Prevent infinite recursion

    // Configurable sensitive field names (case-insensitive, thread-safe)
    private static final Set<String> SENSITIVE_FIELDS = Collections.synchronizedSet(
            new HashSet<>(Set.of("password", "passwd", "pwd", "secret", "token", "apikey", "api_key",
                    "apisecret", "api_secret", "accesstoken", "access_token", "refreshtoken", "refresh_token",
                    "bearertoken", "bearer_token", "privatekey", "private_key", "secretkey", "secret_key",
                    "creditcard", "credit_card", "cardnumber", "card_number", "cvv", "cvc", "pin", "ssn",
                    "authorization", "auth", "credential", "otp", "totp", "mfa")));

    // Precompiled regex patterns for better performance (thread-safe)
    private static final Map<String, Pattern> FIELD_PATTERNS = new ConcurrentHashMap<>();

    // Thread-safe cache for field reflection
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    static {
        initializePatterns();
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        return mapper;
    }

    private static void initializePatterns() {
        for (String field : SENSITIVE_FIELDS) {
            compilePatternForField(field);
        }
    }

    private static void compilePatternForField(String field) {
        // Match both quoted and unquoted values, handle snake_case and camelCase
        String regex = String.format("(\"%s\"\\s*:\\s*)(\"[^\"]*\"|[^,}\\]\\s]+)", Pattern.quote(field));
        FIELD_PATTERNS.put(field, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    }

    /**
     * Main masking method - supports all object types.
     *
     * @param body Object to mask (String, DTO, Array, Collection, Map, Optional,
     *             etc.)
     * @return Masked object (original is not modified)
     */
    public static Object maskPasswordJson(Object body) {
        return maskWithDepth(body, 0);
    }

    private static Object maskWithDepth(Object body, int depth) {
        if (body == null || depth > MAX_DEPTH) {
            return body;
        }

        try {
            return switch (body) {
                case String s -> maskSensitiveFieldsInString(s);
                case Optional<?> opt -> opt.map(v -> maskWithDepth(v, depth + 1)).orElse(null);
                case Object[] arr -> maskArray(arr, depth);
                case Collection<?> coll -> maskCollection(coll, depth);
                case Map<?, ?> map -> maskMap(map, depth);
                case Enum<?> e -> e.name();
                case Number n -> n;
                case Boolean b -> b;
                case Character c -> c;
                case Temporal t -> t.toString();
                case Date d -> d.toString();
                default -> maskCustomObject(body, depth);
            };
        } catch (Exception e) {
            LOG.debug("Masking error: {}", e.getMessage());
            return safeToString(body);
        }
    }

    /**
     * Masks sensitive fields in JSON string.
     */
    private static String maskSensitiveFieldsInString(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        String masked = json;
        for (Pattern pattern : FIELD_PATTERNS.values()) {
            masked = pattern.matcher(masked).replaceAll("$1\"" + MASK + "\"");
        }
        return masked;
    }

    /**
     * Masks sensitive fields in array (immutable).
     */
    private static Object[] maskArray(Object[] arr, int depth) {
        return Arrays.stream(arr).map(item -> maskWithDepth(item, depth + 1)).toArray();
    }

    /**
     * Masks sensitive fields in collection (immutable).
     */
    private static List<?> maskCollection(Collection<?> collection, int depth) {
        return collection.stream().map(item -> maskWithDepth(item, depth + 1)).toList();
    }

    /**
     * Masks sensitive fields in map (immutable).
     */
    private static Map<?, ?> maskMap(Map<?, ?> map, int depth) {
        Map<Object, Object> masked = new HashMap<>(map.size());

        map.forEach(
                (key, value) -> {
                    if (key != null && isSensitiveField(key.toString())) {
                        masked.put(key, MASK);
                    } else {
                        masked.put(key, maskWithDepth(value, depth + 1));
                    }
                });

        return masked;
    }

    /**
     * Masks sensitive fields in DTO/POJO objects.
     */
    private static Object maskCustomObject(Object obj, int depth) {
        if (!isCustomObject(obj)) {
            return safeToString(obj);
        }

        try {
            // Clone and mask
            Object cloned = cloneObject(obj);
            if (cloned == null) {
                return safeToString(obj);
            }

            List<Field> fields = getCachedFields(cloned.getClass());
            for (Field field : fields) {
                maskFieldIfSensitive(cloned, field, depth);
            }

            return OBJECT_MAPPER.writeValueAsString(cloned);
        } catch (Exception e) {
            LOG.debug("Object masking error: {}", e.getMessage());
            return safeToString(obj);
        }
    }

    private static void maskFieldIfSensitive(Object obj, Field field, int depth) {
        try {
            field.setAccessible(true);
            Object value = field.get(obj);

            if (value == null) {
                return;
            }

            if (isSensitiveField(field.getName())) {
                field.set(obj, MASK);
            } else if (isCustomObject(value) || value instanceof Collection || value instanceof Map) {
                // Recursively mask nested objects
                Object maskedValue = maskWithDepth(value, depth + 1);
                if (maskedValue instanceof String) {
                    // Already serialized, skip
                } else {
                    field.set(obj, maskedValue);
                }
            }
        } catch (Exception e) {
            LOG.trace("Field masking error: {} - {}", field.getName(), e.getMessage());
        }
    }

    /**
     * Clones object using JSON serialization.
     */
    private static Object cloneObject(Object obj) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(obj);
            return OBJECT_MAPPER.readValue(json, obj.getClass());
        } catch (Exception e) {
            LOG.debug("Cloning error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets all fields including inherited ones (with caching).
     */
    private static List<Field> getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(
                clazz,
                k -> {
                    List<Field> fields = new ArrayList<>();
                    Class<?> current = clazz;

                    while (current != null && current != Object.class) {
                        for (Field field : current.getDeclaredFields()) {
                            // Skip static and synthetic fields
                            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                                    && !field.isSynthetic()) {
                                fields.add(field);
                            }
                        }
                        current = current.getSuperclass();
                    }

                    return Collections.unmodifiableList(fields);
                });
    }

    /**
     * Checks if field name is sensitive (case-insensitive, contains).
     */
    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }

        String lower = fieldName.toLowerCase();
        // Normalize: remove underscores for comparison
        String normalized = lower.replace("_", "");

        for (String sensitive : SENSITIVE_FIELDS) {
            String normalizedSensitive = sensitive.replace("_", "");
            if (normalized.contains(normalizedSensitive) || lower.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if object is a custom DTO/POJO.
     */
    private static boolean isCustomObject(Object obj) {
        if (obj == null) {
            return false;
        }

        Class<?> clazz = obj.getClass();
        String packageName = clazz.getPackageName();

        return !clazz.isPrimitive()
                && !clazz.isArray()
                && !clazz.isEnum()
                && !packageName.startsWith("java.")
                && !packageName.startsWith("javax.")
                && !packageName.startsWith("jakarta.")
                && !packageName.startsWith("sun.")
                && !packageName.startsWith("jdk.")
                && !packageName.startsWith("com.fasterxml.")
                && !(obj instanceof Number)
                && !(obj instanceof Boolean)
                && !(obj instanceof Character)
                && !(obj instanceof CharSequence)
                && !(obj instanceof Temporal)
                && !(obj instanceof Date);
    }

    /**
     * Safe toString - prevents NPE and exceptions.
     */
    private static String safeToString(Object obj) {
        try {
            return obj != null ? obj.toString() : "null";
        } catch (Exception e) {
            return "[toString error]";
        }
    }

    /**
     * Adds a new sensitive field name (at runtime).
     *
     * @param fieldName Field name to add
     */
    public static void addSensitiveField(String fieldName) {
        if (fieldName != null && !fieldName.isBlank()) {
            String lower = fieldName.toLowerCase().trim();
            SENSITIVE_FIELDS.add(lower);
            compilePatternForField(lower);
        }
    }

    /**
     * Adds multiple sensitive fields.
     *
     * @param fieldNames Field names to add
     */
    public static void addSensitiveFields(String... fieldNames) {
        for (String field : fieldNames) {
            addSensitiveField(field);
        }
    }

    /**
     * Returns the list of sensitive fields (immutable copy).
     */
    public static Set<String> getSensitiveFields() {
        return Set.copyOf(SENSITIVE_FIELDS);
    }

    /**
     * Clears the field cache (useful for testing).
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
    }

    // Private constructor - utility class
    private MaskPasswordUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
