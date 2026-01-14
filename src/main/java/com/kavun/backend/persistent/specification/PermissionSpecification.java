package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.user.Permission;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Specification class for Permission entity filtering operations.
 * Extends BaseSpecification to inherit common filtering capabilities.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class PermissionSpecification extends BaseSpecification<Permission> {

    public Specification<Permission> search(Map<String, Object> search) {
        Specification<Permission> specification = PermissionSpecification.conjunction();
        if (search.containsKey("entityType") && !search.get("entityType").toString().isEmpty()) {
            String entityType = search.get("entityType").toString();
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                            root.get("entityType"), entityType))));
        }
        if (search.containsKey("entityId") && !search.get("entityId").toString().isEmpty()) {
            String entityId = search.get("entityId").toString();
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                            root.get("entityId"), entityId))));
        }
        if (search.containsKey("pageActionId")) {
            Long pageActionId = Long.valueOf(search.get("pageActionId").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                            root.get("pageAction").get("id"), pageActionId))));
        }
        if (search.containsKey("deleted")) {
            Boolean deleted = Boolean.valueOf(search.get("deleted").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"), deleted))));
        }
        if (search.containsKey("expiresAt")) {
            LocalDateTime expiresAt = LocalDateTime.parse(search.get("expiresAt").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("expiresAt"), expiresAt))));
        }
        if (search.containsKey("granted")) {
            Boolean granted = Boolean.valueOf(search.get("granted").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("granted"), granted))));
        }
        return specification;
    }
}
