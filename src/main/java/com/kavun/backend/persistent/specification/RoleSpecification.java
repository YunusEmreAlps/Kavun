package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.user.Role;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Specification class for Role entity filtering operations.
 * Extends BaseSpecification to inherit common filtering capabilities.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class RoleSpecification extends BaseSpecification<Role> {

  public Specification<Role> search(Map<String, Object> search) {
    Specification<Role> specification = RoleSpecification.conjunction();
    if (search.containsKey("name") && !search.get("name").toString().isEmpty()) {
      specification = specification.and(
          (((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
              "%" + search.get("name").toString().toLowerCase() + "%"))));
    }
    if (search.containsKey("description") && !search.get("description").toString().isEmpty()) {
      specification = specification.and(
          (((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("description")),
              "%" + search.get("description").toString().toLowerCase() + "%"))));
    }
    if (search.containsKey("deleted") && !search.get("deleted").toString().isEmpty()) {
      specification = specification.and(
          (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"),
              Boolean.parseBoolean(search.get("deleted").toString())))));
    }
    if (search.containsKey("createdAt") && !search.get("createdAt").toString().isEmpty()) {
      specification = specification.and(
          (((root, query, criteriaBuilder) -> criteriaBuilder.and(
              criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"),
                  LocalDateTime.parse(search.get("createdAt").toString() + " 00:00:00",
                      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
              criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"),
                  LocalDateTime.parse(search.get("createdAt").toString() + " 23:59:59",
                      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))))));
    }
    return specification;
  }
}
