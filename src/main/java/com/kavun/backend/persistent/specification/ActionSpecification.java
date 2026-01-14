package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.user.Action;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Specification class for Action entity filtering operations.
 * Extends BaseSpecification to inherit common filtering capabilities.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class ActionSpecification extends BaseSpecification<Action> {

    public Specification<Action> search(Map<String, Object> search) {
        Specification<Action> specification = ActionSpecification.conjunction();
        if (search.containsKey("code") && !search.get("code").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("code")),
                            "%" + search.get("code").toString().toLowerCase() + "%"))));
        }
        if (search.containsKey("name") && !search.get("name").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("name")),
                            "%" + search.get("name").toString().toLowerCase() + "%"))));
        }
        if (search.containsKey("type") && !search.get("type").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"),
                            search.get("type").toString()))));
        }
        return specification;
    }
}
