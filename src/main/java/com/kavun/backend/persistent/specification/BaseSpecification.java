package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.base.BaseEntity;
import org.springframework.data.jpa.domain.Specification;


/**
 * Base specification class for common entity filtering operations.
 * Provides reusable predicates for BaseEntity fields.
 *
 * @param <T> Entity type that extends BaseEntity
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public abstract class BaseSpecification<T extends BaseEntity> {
  public static <T> Specification<T> conjunction() {
    return (root, query, cb) -> cb.conjunction();
  }
}
