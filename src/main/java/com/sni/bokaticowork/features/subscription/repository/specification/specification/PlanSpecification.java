package com.sni.bokaticowork.features.subscription.repository.specification.specification;

import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionPlan;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.PlanSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlanSpecification {

    private PlanSpecification() {
    }

    public static Specification<SubscriptionPlan> search(PlanSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getCode())) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + criteria.getCode().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(criteria.getName())) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + criteria.getName().trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (criteria.getPlanType() != null) {
                predicates.add(cb.equal(root.get("planType"), criteria.getPlanType()));
            }
            if (criteria.getTargetAudience() != null) {
                predicates.add(cb.equal(root.get("targetAudience"), criteria.getTargetAudience()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getVisible() != null) {
                predicates.add(cb.equal(root.get("visible"), criteria.getVisible()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
