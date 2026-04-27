package com.sni.bokaticowork.features.subscription.repository.specification.specification;

import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PassSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class PassSpecification {

    private PassSpecification() {
    }

    public static Specification<Pass> search(PassSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getOwnerType() != null) {
                predicates.add(cb.equal(root.get("ownerType"), criteria.getOwnerType()));
            }
            if (StringUtils.hasText(criteria.getOwnerCode())) {
                predicates.add(cb.equal(root.get("ownerCode"), criteria.getOwnerCode().trim()));
            }
            if (criteria.getPassType() != null) {
                predicates.add(cb.equal(root.get("passType"), criteria.getPassType()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getExpiringBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("validUntil"), criteria.getExpiringBefore()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
