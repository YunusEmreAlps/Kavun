package com.kavun.backend.persistent.specification;

import com.kavun.exception.InvalidFilterException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ClassUtils;

/**
 * Builds a query from list filters (query parameters or a search body), so services only declare
 * which fields may be filtered. For an allowed field {@code f}:
 *
 * <ul>
 *   <li>{@code f=value}: text fields match when they contain the value, ignoring case and accents
 *       in any language (see {@link SearchUtils}); other types must be equal. A date ({@code
 *       2026-10-16}) on a date-time field matches that day.
 *   <li>{@code fFrom=value} / {@code fTo=value}: inclusive range; a date as upper bound covers
 *       the whole day.
 *   <li>{@code fId=value}: for a many-to-one relation {@code f}, matches the related id (e.g.
 *       {@code parentId}).
 * </ul>
 *
 * <p>Parameters for other fields are ignored, which also covers paging parameters. The allow-list
 * keeps sensitive fields such as password hashes from ever being queried. A value that does not fit
 * its field raises {@link InvalidFilterException}.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class FilterSpecification {

  private static final String FROM = "From";
  private static final String TO = "To";
  private static final String ID = "Id";
  private static final char LIKE_ESCAPE = '\\';
  /** The SQL function that folds a column like {@link SearchUtils#fold(String)}. */
  private static final String SEARCH_FOLD = "search_fold";
  private static final ConversionService CONVERSION = DefaultConversionService.getSharedInstance();

  private FilterSpecification() {}

  /**
   * Builds the specification.
   *
   * @param filters the filter parameters
   * @param filterableFields the entity fields that may be filtered
   * @param <T> the entity type
   * @return a specification matching all filters
   */
  public static <T> Specification<T> of(Map<String, ?> filters, Set<String> filterableFields) {
    Specification<T> specification = (root, query, cb) -> cb.conjunction();
    for (Map.Entry<String, ?> filter : filters.entrySet()) {
      String value = StringUtils.trimToNull(Objects.toString(filter.getValue(), null));
      Specification<T> condition = condition(filter.getKey(), value, filterableFields);
      if (condition != null) {
        specification = specification.and(condition);
      }
    }
    return specification;
  }

  private static <T> Specification<T> condition(String key, String value, Set<String> fields) {
    if (value == null) {
      return null;
    }
    if (fields.contains(key)) {
      return (root, query, cb) -> matches(cb, root.get(key), key, value);
    }
    String from = StringUtils.removeEnd(key, FROM);
    if (!from.equals(key) && fields.contains(from)) {
      return (root, query, cb) -> bound(cb, root.get(from), key, value, true);
    }
    String to = StringUtils.removeEnd(key, TO);
    if (!to.equals(key) && fields.contains(to)) {
      return (root, query, cb) -> bound(cb, root.get(to), key, value, false);
    }
    String relation = StringUtils.removeEnd(key, ID);
    if (!relation.equals(key) && fields.contains(relation)) {
      return (root, query, cb) ->
          cb.equal(root.get(relation).get("id"), convert(value, Long.class, key));
    }
    return null;
  }

  private static Predicate matches(CriteriaBuilder cb, Path<?> path, String key, String value) {
    Class<?> type = path.getJavaType();
    if (type == String.class) {
      String pattern = "%" + escapeLike(SearchUtils.fold(value)) + "%";
      Expression<String> folded = cb.function(SEARCH_FOLD, String.class, path.as(String.class));
      return cb.like(folded, pattern, LIKE_ESCAPE);
    }
    if (type == LocalDateTime.class && isDate(value)) {
      LocalDate day = (LocalDate) convert(value, LocalDate.class, key);
      return cb.between(path.as(LocalDateTime.class), day.atStartOfDay(), day.atTime(LocalTime.MAX));
    }
    return cb.equal(path, convert(value, type, key));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Predicate bound(
      CriteriaBuilder cb, Path<?> path, String key, String value, boolean lower) {
    Class<?> type = ClassUtils.resolvePrimitiveIfNecessary(path.getJavaType());
    if (!Comparable.class.isAssignableFrom(type) || type == Boolean.class) {
      throw new InvalidFilterException("Filter " + key + " needs a field that can be ordered");
    }
    Comparable bound;
    if (type == LocalDateTime.class && isDate(value)) {
      LocalDate day = (LocalDate) convert(value, LocalDate.class, key);
      bound = lower ? day.atStartOfDay() : day.atTime(LocalTime.MAX);
    } else {
      bound = (Comparable) convert(value, type, key);
    }
    Expression<Comparable> expression = (Expression<Comparable>) path;
    return lower
        ? cb.greaterThanOrEqualTo(expression, bound)
        : cb.lessThanOrEqualTo(expression, bound);
  }

  private static Object convert(String value, Class<?> type, String key) {
    try {
      if (type.isEnum()) {
        return Arrays.stream(type.getEnumConstants())
            .filter(constant -> ((Enum<?>) constant).name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> invalid(key, value));
      }
      if (ClassUtils.resolvePrimitiveIfNecessary(type) == Boolean.class) {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
          throw invalid(key, value);
        }
        return Boolean.parseBoolean(value);
      }
      if (type == LocalDateTime.class) {
        return isDate(value) ? LocalDate.parse(value).atStartOfDay() : LocalDateTime.parse(value);
      }
      if (type == LocalDate.class) {
        return LocalDate.parse(value);
      }
      return CONVERSION.convert(value, type);
    } catch (ConversionException | DateTimeParseException e) {
      throw invalid(key, value);
    }
  }

  private static boolean isDate(String value) {
    return value.length() == 10 && value.charAt(4) == '-' && value.charAt(7) == '-';
  }

  private static String escapeLike(String value) {
    return value
        .replace(String.valueOf(LIKE_ESCAPE), String.valueOf(LIKE_ESCAPE) + LIKE_ESCAPE)
        .replace("%", LIKE_ESCAPE + "%")
        .replace("_", LIKE_ESCAPE + "_");
  }

  private static InvalidFilterException invalid(String key, String value) {
    return new InvalidFilterException("Invalid value '" + value + "' for filter " + key);
  }
}
