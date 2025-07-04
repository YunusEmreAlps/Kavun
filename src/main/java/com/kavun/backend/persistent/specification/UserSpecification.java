package com.kavun.backend.persistent.specification;

import com.kavun.backend.persistent.domain.user.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserSpecification {

    public Specification<User> search(Map<String, Object> search) {
        Specification<User> spec = Specification.where(null);

        if (search == null || search.isEmpty()) {
            return spec;
        }

        if (search.containsKey("publicId")) {
            String publicId = String.valueOf(search.get("publicId"));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("publicId"), publicId));
        }
        if (search.containsKey("username")) {
            String username = String.valueOf(search.get("username"));
            spec = spec.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
        }
        if (search.containsKey("email")) {
            String email = String.valueOf(search.get("email"));
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
        }
        if (search.containsKey("firstName")) {
            String firstName = String.valueOf(search.get("firstName"));
            spec = spec.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
        }
        if (search.containsKey("lastName")) {
            String lastName = String.valueOf(search.get("lastName"));
            spec = spec.and(
                    (root, query, cb) -> cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
        }
        if (search.containsKey("phone")) {
            String phone = String.valueOf(search.get("phone"));
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("phone")), "%" + phone.toLowerCase() + "%"));
        }
        if (search.containsKey("enabled")) {
            Boolean enabled = Boolean.valueOf(String.valueOf(search.get("enabled")));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("enabled"), enabled));
        }
        if (search.containsKey("deletedAt")) {
            String deletedAt = String.valueOf(search.get("deletedAt"));
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("deletedAt"), deletedAt));
        }
        if (search.containsKey("deleted")) {
            Boolean deleted = Boolean.valueOf(String.valueOf(search.get("deleted")));
            spec = spec.and((root, query, cb) -> cb.equal(root.get("deleted"), deleted));
        }
        return spec;
    }
}
