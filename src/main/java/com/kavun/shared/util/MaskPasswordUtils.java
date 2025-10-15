package com.kavun.shared.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for masking sensitive information such as passwords in logs.
 *
 * @author Yunus Emre Alpu
 * @version 2.0
 * @since 1.0
 */
@Slf4j
public class MaskPasswordUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MASK = "******";

    // Configurable sensitive field names (case-insensitive)
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "secret", "token", "apikey", "apisecret",
            "accesstoken", "refreshtoken", "privatekey", "creditcard",
            "cvv", "pin", "ssn", "authorization");

    // Precompiled regex patterns for better performance
    private static final Map<String, Pattern> FIELD_PATTERNS = new HashMap<>();

    static {
        // Compile patterns once at class loading
        for (String field : SENSITIVE_FIELDS) {
            // Match both quoted and unquoted values
            String regex = String.format(
                    "(\"%s\"\\s*:\\s*)(\"[^\"]*\"|[^,}\\]\\s]+)",
                    field);
            FIELD_PATTERNS.put(field, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }
    }

    // Cache for field reflection to improve performance
    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new HashMap<>();

    /**
     * Main method to mask sensitive data in any object
     *
     * @param body The object to mask (String, DTO, Array, Collection, etc.)
     * @return Masked object (immutable - original is not modified)
     */
    public static Object maskPasswordJson(Object body) {
        if (body == null) {
            return null;
        }

        try {
            // Handle JSON strings
            if (body instanceof String) {
                return maskSensitiveFieldsInString((String) body);
            }

            // Handle arrays
            if (body.getClass().isArray()) {
                return maskArray((Object[]) body);
            }

            // Handle collections
            if (body instanceof Collection) {
                return maskCollection((Collection<?>) body);
            }

            // Handle maps
            if (body instanceof Map) {
                return maskMap((Map<?, ?>) body);
            }

            // Handle DTOs/POJOs
            if (isCustomObject(body)) {
                return maskObjectFields(body);
            }

            // Return primitive types and standard Java objects as-is
            return body;

        } catch (Exception e) {
            LOG.debug("Error masking sensitive data: {}", e.getMessage());
            return body; // Return original if masking fails
        }
    }

    /**
     * Mask sensitive fields in JSON string using precompiled regex
     */
    private static String maskSensitiveFieldsInString(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        String maskedJson = json;

        // Use precompiled patterns for better performance
        for (Map.Entry<String, Pattern> entry : FIELD_PATTERNS.entrySet()) {
            maskedJson = entry.getValue()
                    .matcher(maskedJson)
                    .replaceAll("$1\"" + MASK + "\"");
        }

        return maskedJson;
    }

    /**
     * Mask sensitive fields in array (immutable)
     */
    private static Object[] maskArray(Object[] arr) {
        return Arrays.stream(arr)
                .map(MaskPasswordUtils::maskPasswordJson)
                .toArray();
    }

    /**
     * Mask sensitive fields in collection (immutable)
     */
    private static Collection<?> maskCollection(Collection<?> collection) {
        return collection.stream()
                .map(MaskPasswordUtils::maskPasswordJson)
                .collect(Collectors.toList());
    }

    /**
     * Mask sensitive fields in map (immutable)
     */
    private static Map<?, ?> maskMap(Map<?, ?> map) {
        Map<Object, Object> maskedMap = new HashMap<>();

        map.forEach((key, value) -> {
            String keyStr = key != null ? key.toString().toLowerCase() : "";

            // Check if key is a sensitive field
            if (isSensitiveField(keyStr)) {
                maskedMap.put(key, MASK);
            } else {
                maskedMap.put(key, maskPasswordJson(value));
            }
        });

        return maskedMap;
    }

    /**
     * Mask sensitive fields in DTO/POJO using reflection (immutable)
     */
    private static Object maskObjectFields(Object obj) {
        try {
            // Clone object to avoid modifying original
            Object cloned = cloneObject(obj);

            // Get fields (use cache for performance)
            List<Field> fields = getCachedFields(cloned.getClass());

            // Mask sensitive fields
            for (Field field : fields) {
                if (isSensitiveField(field.getName())) {
                    field.setAccessible(true);
                    field.set(cloned, MASK);
                }
            }

            // Convert to JSON string for logging
            return OBJECT_MAPPER.writeValueAsString(cloned);

        } catch (Exception e) {
            LOG.debug("Error masking object fields: {}", e.getMessage());
            return obj.toString();
        }
    }

    /**
     * Clone object using JSON serialization (simple and safe)
     */
    private static Object cloneObject(Object obj) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(obj);
            return OBJECT_MAPPER.readValue(json, obj.getClass());
        } catch (Exception e) {
            LOG.debug("Error cloning object: {}", e.getMessage());
            return obj; // Return original if cloning fails
        }
    }

    /**
     * Get all fields including inherited ones (with caching)
     */
    private static List<Field> getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            List<Field> fields = new ArrayList<>();
            Class<?> currentClass = clazz;

            while (currentClass != null && currentClass != Object.class) {
                fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
                currentClass = currentClass.getSuperclass();
            }

            return Collections.unmodifiableList(fields);
        });
    }

    /**
     * Check if field name is sensitive (case-insensitive)
     */
    private static boolean isSensitiveField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }

        String lowerFieldName = fieldName.toLowerCase();
        return SENSITIVE_FIELDS.stream()
                .anyMatch(lowerFieldName::contains);
    }

    /**
     * Check if object is a custom DTO/POJO
     */
    private static boolean isCustomObject(Object obj) {
        if (obj == null) {
            return false;
        }

        Class<?> clazz = obj.getClass();
        String packageName = clazz.getPackageName();

        // Exclude primitives, wrappers, and standard Java classes
        return !clazz.isPrimitive()
                && !clazz.isArray()
                && !packageName.startsWith("java.")
                && !packageName.startsWith("javax.")
                && !packageName.startsWith("sun.")
                && !packageName.startsWith("jdk.")
                && !(obj instanceof Number)
                && !(obj instanceof Boolean)
                && !(obj instanceof Character)
                && !(obj instanceof CharSequence);
    }

    /**
     * Add custom sensitive field name
     */
    public static void addSensitiveField(String fieldName) {
        if (fieldName != null && !fieldName.isEmpty()) {
            SENSITIVE_FIELDS.add(fieldName.toLowerCase());

            // Add pattern for new field
            String regex = String.format(
                    "(\"%s\"\\s*:\\s*)(\"[^\"]*\"|[^,}\\]\\s]+)",
                    fieldName.toLowerCase());
            FIELD_PATTERNS.put(
                    fieldName.toLowerCase(),
                    Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
        }
    }

    /**
     * Clear field cache (useful for testing)
     */
    public static void clearCache() {
        FIELD_CACHE.clear();
    }

    private MaskPasswordUtils() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("Utility class");
    }
}
