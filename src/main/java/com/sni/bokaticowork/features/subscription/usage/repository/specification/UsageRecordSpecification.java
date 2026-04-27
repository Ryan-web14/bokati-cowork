package com.sni.bokaticowork.features.subscription.usage.repository.specification;

import com.sni.bokaticowork.features.subscription.usage.model.UsageRecord;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class UsageRecordSpecification {
    private UsageRecordSpecification() {
    }

    public static Specification<UsageRecord> search(UsageRecordCriteria criteria) {
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
            if (StringUtils.hasText(criteria.getEntitlementCode())) {
                predicates.add(cb.equal(cb.lower(root.get("entitlementCode")), criteria.getEntitlementCode().trim().toLowerCase()));
            }
            if (StringUtils.hasText(criteria.getReferenceType())) {
                predicates.add(cb.equal(root.get("referenceType"), criteria.getReferenceType().trim()));
            }
            if (StringUtils.hasText(criteria.getReferenceId())) {
                predicates.add(cb.equal(root.get("referenceId"), criteria.getReferenceId().trim()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
