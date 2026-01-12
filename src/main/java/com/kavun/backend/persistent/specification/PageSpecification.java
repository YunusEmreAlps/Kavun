package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.user.WebPage;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Specification class for Page entity filtering operations.
 * Extends BaseSpecification to inherit common filtering capabilities.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Component
public class PageSpecification extends BaseSpecification<WebPage> {

    public Specification<WebPage> search(Map<String, Object> search) {
        Specification<WebPage> specification = PageSpecification.conjunction();
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
        if (search.containsKey("url") && !search.get("url").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("url")),
                            "%" + search.get("url").toString().toLowerCase() + "%"))));
        }
        if (search.containsKey("icon") && !search.get("icon").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("icon")),
                            "%" + search.get("icon").toString().toLowerCase() + "%"))));
        }
        if (search.containsKey("description") && !search.get("description").toString().isEmpty()) {
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("description")),
                            "%" + search.get("description").toString().toLowerCase() + "%"))));
        }
        if (search.containsKey("deleted")) {
            Boolean deleted = Boolean.valueOf(search.get("deleted").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"), deleted))));
        }
        if (search.containsKey("parentId")) {
            Long parentId = Long.valueOf(search.get("parentId").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("parent").get("id"),
                            parentId))));
        }
        if (search.containsKey("level")) {
            Integer level = Integer.valueOf(search.get("level").toString());
            specification = specification.and(
                    (((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("level"), level))));
        }

        return specification;
    }
}
