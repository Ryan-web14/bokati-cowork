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

            // Lot R5b : par site et par etage.
            if (StringUtils.hasText(criteria.getSiteCode())) {
                predicates.add(cb.equal(cb.upper(root.get("site").get("code")), criteria.getSiteCode().trim().toUpperCase()));
            }
            if (StringUtils.hasText(criteria.getFloorCode())) {
                predicates.add(cb.equal(cb.upper(root.get("floor").get("code")), criteria.getFloorCode().trim().toUpperCase()));
            }
            // Lot R4 : par code, comme partout ailleurs dans l'API.
            if (StringUtils.hasText(criteria.getTypeCode())) {
                predicates.add(cb.equal(cb.upper(root.get("resourceType").get("code")), criteria.getTypeCode().trim().toUpperCase()));
            }
            if (StringUtils.hasText(criteria.getGroupCode())) {
                predicates.add(cb.equal(cb.upper(root.get("resourceGroup").get("code")), criteria.getGroupCode().trim().toUpperCase()));
            }
            if (StringUtils.hasText(criteria.getPolicyCode())) {
                predicates.add(cb.equal(cb.upper(root.get("resourcePolicy").get("code")), criteria.getPolicyCode().trim().toUpperCase()));
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

            // Lot B10 : ressources reservees visibles seulement par leur audience.
            if (criteria.getRestrictedVisibleIds() != null) {
                Predicate notRestricted = cb.or(cb.isNull(root.get("visibilityMode")),
                        cb.notEqual(root.get("visibilityMode"), com.sni.bokaticowork.features.ressource.model.ResourceVisibility.RESTRICTED));
                predicates.add(criteria.getRestrictedVisibleIds().isEmpty() ? notRestricted
                        : cb.or(notRestricted, root.get("id").in(criteria.getRestrictedVisibleIds())));
            }

            return cb.and(predicates.toArray(new Predicate[0]));

        };

    }

}
