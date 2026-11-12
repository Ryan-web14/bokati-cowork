package com.sni.bokaticowork.features.inventory.procurement.suggestion.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Historique des choix de fournisseurs, par fiche d'inventaire.
 *
 * <p>Une ligne compte pour un fournisseur quand elle porte son code, ou, faute de code (lignes
 * anterieures), quand son texte libre {@code supplier} est exactement son nom (sans tenir compte de
 * la casse ni des espaces autour). Les commandes comptables comptent par le fournisseur comptable
 * relie a la fiche ; les commandes de stock par leur fournisseur.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierSuggestionHistory {

    private static final String BY_NATURE = """
            WITH history AS (
                SELECT supplier_code, supplier FROM fund_need_line WHERE nature_code = :nature
                UNION ALL
                SELECT supplier_code, supplier FROM fund_request_line WHERE nature_code = :nature
                UNION ALL
                SELECT supplier_code, supplier FROM expense_justification_line WHERE nature_code = :nature AND status <> 'REJECTED'
            )
            SELECT s.id, COUNT(*)
            FROM history h
                     JOIN inventory_supplier s
                          ON s.supplier_code = h.supplier_code
                              OR (h.supplier_code IS NULL AND h.supplier IS NOT NULL AND UPPER(TRIM(h.supplier)) = UPPER(TRIM(s.name)))
            GROUP BY s.id
            UNION ALL
            SELECT a.inventory_supplier_id, COUNT(*)
            FROM acc_purchase_order_line l
                     JOIN acc_purchase_order o ON o.id = l.order_id
                     JOIN acc_supplier a ON a.code = o.supplier_code
            WHERE l.nature_code = :nature
              AND o.status <> 'CANCELLED'
              AND a.inventory_supplier_id IS NOT NULL
            GROUP BY a.inventory_supplier_id
            """;

    private static final String BY_INVENTORY_CATEGORY = """
            SELECT o.supplier_id, COUNT(DISTINCT o.id)
            FROM inventory_purchase_order o
                     JOIN inventory_purchase_order_line l ON l.purchase_order_id = o.id
                     JOIN inventory_item i ON i.id = l.item_id
                     JOIN inventory_category c ON c.id = i.category_id
            WHERE c.code = :category
              AND o.status <> 'CANCELLED'
            GROUP BY o.supplier_id
            """;

    private final EntityManager entityManager;

    /** Nombre de fois ou chaque fournisseur a ete retenu sur une ligne de cette nature. */
    public Map<Long, Long> byNature(String natureCode) {
        return counts(BY_NATURE, "nature", natureCode);
    }

    /** Nombre de commandes de stock passees a chaque fournisseur pour des articles de cette categorie. */
    public Map<Long, Long> byInventoryCategory(String categoryCode) {
        return counts(BY_INVENTORY_CATEGORY, "category", categoryCode);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Long> counts(String sql, String parameter, String value) {
        Map<Long, Long> result = new HashMap<>();
        if (value == null) {
            return result;
        }
        List<Object[]> rows = entityManager.createNativeQuery(sql).setParameter(parameter, value).getResultList();
        for (Object[] row : rows) {
            if (row[0] != null) {
                result.merge(((Number) row[0]).longValue(), ((Number) row[1]).longValue(), Long::sum);
            }
        }
        return result;
    }
}
