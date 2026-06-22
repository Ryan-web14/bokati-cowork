package com.sni.bokaticowork.features.reporting.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StockValuationReportRepository {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Object[]> valuationByCategory() {
        Query q = em.createNativeQuery("""
                SELECT
                    ic.code AS category_code,
                    ic.name AS category_name,
                    count(DISTINCT sl.item_id) AS item_count,
                    coalesce(sum(sl.quantity_on_hand), 0) AS total_quantity,
                    coalesce(sum(sl.quantity_on_hand * sl.average_cost), 0) AS total_value
                FROM stock_level sl
                JOIN inventory_item ii ON ii.id = sl.item_id AND ii.active = true
                LEFT JOIN inventory_category ic ON ic.id = ii.category_id
                WHERE sl.quantity_on_hand > 0
                GROUP BY ic.code, ic.name
                ORDER BY total_value DESC
                """);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> valuationByLocation() {
        Query q = em.createNativeQuery("""
                SELECT
                    il.location_code,
                    il.name AS location_name,
                    il.location_type,
                    count(DISTINCT sl.item_id) AS item_count,
                    coalesce(sum(sl.quantity_on_hand), 0) AS total_quantity,
                    coalesce(sum(sl.quantity_on_hand * sl.average_cost), 0) AS total_value
                FROM stock_level sl
                JOIN inventory_location il ON il.id = sl.location_id AND il.active = true
                WHERE sl.quantity_on_hand > 0
                GROUP BY il.location_code, il.name, il.location_type
                ORDER BY total_value DESC
                """);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> lowStockItems() {
        Query q = em.createNativeQuery("""
                SELECT
                    ii.item_code,
                    ii.name AS item_name,
                    ic.name AS category_name,
                    coalesce(sum(sl.quantity_available), 0) AS quantity_available,
                    rr.min_quantity AS reorder_point,
                    rr.reorder_quantity
                FROM inventory_reorder_rule rr
                JOIN inventory_item ii ON ii.id = rr.item_id AND ii.active = true
                LEFT JOIN inventory_category ic ON ic.id = ii.category_id
                LEFT JOIN stock_level sl ON sl.item_id = ii.id
                WHERE rr.active = true
                GROUP BY ii.item_code, ii.name, ic.name, rr.min_quantity, rr.reorder_quantity
                HAVING coalesce(sum(sl.quantity_available), 0) <= rr.min_quantity
                ORDER BY quantity_available ASC
                """);
        return q.getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> deadStock(int inactiveDays) {
        Query q = em.createNativeQuery("""
                SELECT
                    ii.item_code,
                    ii.name AS item_name,
                    ic.name AS category_name,
                    il.name AS location_name,
                    sl.quantity_on_hand,
                    sl.quantity_on_hand * sl.average_cost AS stock_value,
                    sl.last_movement_at,
                    EXTRACT(DAY FROM (NOW() - sl.last_movement_at))::integer AS days_since
                FROM stock_level sl
                JOIN inventory_item ii ON ii.id = sl.item_id AND ii.active = true
                JOIN inventory_location il ON il.id = sl.location_id
                LEFT JOIN inventory_category ic ON ic.id = ii.category_id
                WHERE sl.quantity_on_hand > 0
                  AND sl.last_movement_at < NOW() - CAST(:inactiveDays || ' days' AS interval)
                ORDER BY days_since DESC
                """);
        q.setParameter("inactiveDays", String.valueOf(inactiveDays));
        return q.getResultList();
    }
}
