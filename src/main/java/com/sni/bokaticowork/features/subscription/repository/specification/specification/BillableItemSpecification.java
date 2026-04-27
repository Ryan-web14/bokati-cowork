package com.sni.bokaticowork.features.subscription.repository.specification.specification;

import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.BillableItemSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class BillableItemSpecification {

    private BillableItemSpecification() {
    }

    public static Specification<BillableItem> search(BillableItemSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getSubscriberType() != null) {
                predicates.add(cb.equal(root.get("subscriberType"), criteria.getSubscriberType()));
            }
            if (StringUtils.hasText(criteria.getSubscriberCode())) {
                predicates.add(cb.equal(root.get("subscriberCode"), criteria.getSubscriberCode().trim()));
            }
            if (StringUtils.hasText(criteria.getSourceType())) {
                predicates.add(cb.equal(root.get("sourceType"), criteria.getSourceType().trim()));
            }
            if (StringUtils.hasText(criteria.getSourceId())) {
                predicates.add(cb.equal(root.get("sourceId"), criteria.getSourceId().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
