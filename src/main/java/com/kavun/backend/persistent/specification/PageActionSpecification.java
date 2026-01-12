package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.user.PageAction;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Specification class for PageAction entity filtering operations.
 * Extends BaseSpecification to inherit common filtering capabilities.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class PageActionSpecification extends BaseSpecification<PageAction> {

    public Specification<PageAction> search(Map<String, Object> search) {
        Specification<PageAction> specification = PageActionSpecification.conjunction();
        if (search.containsKey("pageId")) {
            Long pageId = Long.valueOf(search.get("pageId").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("page").get("id"),
                            pageId))));
        }
        if (search.containsKey("actionId")) {
            Long actionId = Long.valueOf(search.get("actionId").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("action").get("id"),
                            actionId))));
        }
        if (search.containsKey("apiEndpoint") && !search.get("apiEndpoint").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("apiEndpoint")),
                            "%" + search.get("apiEndpoint").toString().toLowerCase() + "%"))));
        }
        if (search.containsKey("httpMethod") && !search.get("httpMethod").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(
                            root.get("httpMethod"), search.get("httpMethod").toString()))));
        }
        if (search.containsKey("deleted")) {
            Boolean deleted = Boolean.valueOf(search.get("deleted").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"), deleted))));
        }
        if (search.containsKey("label")) {
            String label = search.get("label").toString();
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("action").get("label"),
                            label))));
        }
        return specification;
    }
}
