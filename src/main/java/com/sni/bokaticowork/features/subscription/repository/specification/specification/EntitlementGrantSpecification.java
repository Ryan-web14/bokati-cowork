package com.sni.bokaticowork.features.subscription.repository.specification.specification;

import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.EntitlementGrantSearchCriteria;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class EntitlementGrantSpecification {

    private EntitlementGrantSpecification() {
    }

    public static Specification<EntitlementGrant> search(EntitlementGrantSearchCriteria criteria) {
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
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getValidAt() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("validFrom"), criteria.getValidAt()));
                predicates.add(cb.or(cb.isNull(root.get("validUntil")), cb.greaterThanOrEqualTo(root.get("validUntil"), criteria.getValidAt())));
            }
            if (StringUtils.hasText(criteria.getEntitlementCode())) {
                var definition = root.join("entitlementDefinition", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(definition.get("code")), criteria.getEntitlementCode().trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
