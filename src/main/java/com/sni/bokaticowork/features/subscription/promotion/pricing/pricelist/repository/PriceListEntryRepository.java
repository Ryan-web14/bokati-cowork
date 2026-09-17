package com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.model.PriceListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PriceListEntryRepository extends JpaRepository<PriceListEntry, Long> {

    @Query("SELECT e FROM PriceListEntry e WHERE e.priceList.id IN :priceListIds")
    List<PriceListEntry> findAllByPriceListIds(@Param("priceListIds") Collection<Long> priceListIds);

    @Query("SELECT e FROM PriceListEntry e WHERE e.priceList.id = :priceListId ORDER BY e.minQuantity DESC")
    List<PriceListEntry> findAllByPriceListId(@Param("priceListId") Long priceListId);
}
