package com.sni.bokaticowork.features.inventory.asset.repository.specification;

import com.sni.bokaticowork.features.inventory.asset.enums.AssetAssigneeType;
import com.sni.bokaticowork.features.inventory.asset.enums.AssetStatus;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AssetSpecification {

    private AssetSpecification() {
    }

    public static Specification<Asset> filters(String itemCode, AssetStatus status, String locationCode,
                                               AssetAssigneeType assignedToType, String assignedToCode) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(itemCode)) {
                predicates.add(cb.equal(root.join("item", JoinType.INNER).get("itemCode"), normalize(itemCode)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(locationCode)) {
                predicates.add(cb.equal(root.join("location", JoinType.LEFT).get("locationCode"), normalize(locationCode)));
            }
            if (assignedToType != null) {
                predicates.add(cb.equal(root.get("assignedToType"), assignedToType));
            }
            if (StringUtils.hasText(assignedToCode)) {
                predicates.add(cb.equal(root.get("assignedToCode"), normalize(assignedToCode)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }
}
