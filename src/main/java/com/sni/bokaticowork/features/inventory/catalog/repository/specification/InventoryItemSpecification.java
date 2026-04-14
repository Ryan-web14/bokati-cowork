package com.sni.bokaticowork.features.inventory.catalog.repository.specification;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class InventoryItemSpecification {

    private InventoryItemSpecification() {
    }

    public static Specification<InventoryItem> filters(String categoryCode, InventoryItemType itemType, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(categoryCode)) {
                predicates.add(cb.equal(root.join("category", JoinType.LEFT).get("code"), normalize(categoryCode)));
            }
            if (itemType != null) {
                predicates.add(cb.equal(root.get("itemType"), itemType));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("[^A-Za-z0-9]+", "-").replaceAll("-+", "-")
                .replaceAll("^-|-$", "").toUpperCase(Locale.ROOT);
    }
}
