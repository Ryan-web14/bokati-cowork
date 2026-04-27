package com.sni.bokaticowork.features.subscription.promotion.repository.specification;

import com.sni.bokaticowork.features.subscription.promotion.model.Promotion;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PromotionSpecification {
    private PromotionSpecification() {
    }

    public static Specification<Promotion> search(PromotionCriteria criteria) {
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
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
