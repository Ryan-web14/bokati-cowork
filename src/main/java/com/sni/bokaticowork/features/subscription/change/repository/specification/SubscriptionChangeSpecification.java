package com.sni.bokaticowork.features.subscription.change.repository.specification;

import com.sni.bokaticowork.features.subscription.change.model.SubscriptionChangeRequest;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class SubscriptionChangeSpecification {
    private SubscriptionChangeSpecification() {
    }

    public static Specification<SubscriptionChangeRequest> search(SubscriptionChangeCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(criteria.getSubscriptionNumber())) {
                predicates.add(cb.equal(root.join("subscription", JoinType.INNER).get("subscriptionNumber"), criteria.getSubscriptionNumber().trim()));
            }
            if (criteria.getChangeType() != null) {
                predicates.add(cb.equal(root.get("changeType"), criteria.getChangeType()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
