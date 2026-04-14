package com.sni.bokaticowork.features.ressource.repository.specification.specification;

import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.repository.specification.criteria.ResourceSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ResourceSpecification {

    private ResourceSpecification() {
    }

    public static Specification<Resource> search(ResourceSearchCriteria criteria) {

        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getCode())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("code")),
                                "%" + criteria.getCode().toLowerCase() + "%"
                        )
                );
            }

            if (criteria.getTypeId() != null) {
                predicates.add(cb.equal(root.get("resourceType").get("id"), criteria.getTypeId()));
            }

            if (criteria.getGroupId() != null) {
                predicates.add(cb.equal(root.get("resourceGroup").get("id"), criteria.getGroupId()));
            }

            if (criteria.getPolicyId() != null) {
                predicates.add(cb.equal(root.get("resourcePolicy").get("id"), criteria.getPolicyId()));
            }

            if (StringUtils.hasText(criteria.getName())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("name")),
                                "%" + criteria.getName().toLowerCase() + "%"
                        )
                );
            }

            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            if (StringUtils.hasText(criteria.getZone())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("zone")),
                                "%" + criteria.getZone().toLowerCase() + "%"
                        )
                );
            }

            if (StringUtils.hasText(criteria.getLocationLabel())) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("locationLabel")),
                                "%" + criteria.getLocationLabel().toLowerCase() + "%"
                        )
                );
            }

            if (criteria.getPortalVisible() != null) {
                predicates.add(cb.equal(root.get("portalVisible"), criteria.getPortalVisible()));
            }

            if (criteria.getBookingEnabled() != null) {
                predicates.add(cb.equal(root.get("bookingEnabled"), criteria.getBookingEnabled()));
            }

            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }

            if (criteria.getMinCapacity() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("capacity"), criteria.getMinCapacity())
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));

        };

    }

}
