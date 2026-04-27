package com.sni.bokaticowork.features.subscription.repository.specification.specification;

import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.SubscriptionSearchCriteria;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SubscriptionSpecification {

    private SubscriptionSpecification() {
    }

    public static Specification<Subscription> search(SubscriptionSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getSubscriberType() != null) {
                predicates.add(cb.equal(root.get("subscriberType"), criteria.getSubscriberType()));
            }
            if (StringUtils.hasText(criteria.getSubscriberCode())) {
                predicates.add(cb.equal(root.get("subscriberCode"), criteria.getSubscriberCode().trim()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getNextBillingBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("nextBillingDate"), criteria.getNextBillingBefore()));
            }
            if (StringUtils.hasText(criteria.getPlanCode())) {
                var plan = root.join("planVersion", JoinType.INNER).join("plan", JoinType.INNER);
                predicates.add(cb.equal(cb.lower(plan.get("code")), criteria.getPlanCode().trim().toLowerCase()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
