package com.sni.bokaticowork.features.booking.repository.specification;

import com.sni.bokaticowork.features.booking.model.Booking;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecification {

    private BookingSpecification() {
    }

    public static Specification<Booking> search(BookingCriteria criteria) {
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
            if (StringUtils.hasText(criteria.getResourceCode())) {
                predicates.add(cb.equal(cb.lower(root.get("resource").get("code")), criteria.getResourceCode().trim().toLowerCase()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getStartedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), criteria.getStartedFrom()));
            }
            if (criteria.getStartedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), criteria.getStartedTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
