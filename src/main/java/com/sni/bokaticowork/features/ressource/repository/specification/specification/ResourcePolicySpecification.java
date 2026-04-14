package com.sni.bokaticowork.features.ressource.repository.specification.specification;

import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourcePolicyCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ResourcePolicySpecification {

    private ResourcePolicySpecification() {
    }

    public static Specification<ResourcePolicy> search(ResourcePolicyCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getCode())) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + criteria.getCode().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(criteria.getName())) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + criteria.getName().toLowerCase() + "%"));
            }

            if (criteria.getAllowCancellation() != null) {
                predicates.add(cb.equal(root.get("allowCancellation"), criteria.getAllowCancellation()));
            }

            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
