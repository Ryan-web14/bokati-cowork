package com.sni.bokaticowork.features.subscription.addon.repository.specification;

import com.sni.bokaticowork.features.subscription.addon.model.SubscriptionAddon;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SubscriptionAddonSpecification {
    private SubscriptionAddonSpecification() {
    }

    public static Specification<SubscriptionAddon> search(SubscriptionAddonCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(criteria.getSubscriptionNumber())) {
                predicates.add(cb.equal(root.join("subscription", JoinType.INNER).get("subscriptionNumber"), criteria.getSubscriptionNumber().trim()));
            }
            if (StringUtils.hasText(criteria.getPlanCode())) {
                predicates.add(cb.equal(cb.lower(root.join("planVersion", JoinType.INNER).join("plan", JoinType.INNER).get("code")), criteria.getPlanCode().trim().toLowerCase()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
