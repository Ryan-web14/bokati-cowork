package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetEventRepository extends JpaRepository<FixedAssetEvent, Long> {

    List<FixedAssetEvent> findAllByAssetIdOrderByCreatedAtAscIdAsc(Long assetId);

    List<FixedAssetEvent> findAllByKindAndEventDateBetween(String kind, java.time.LocalDate from, java.time.LocalDate to);
}
