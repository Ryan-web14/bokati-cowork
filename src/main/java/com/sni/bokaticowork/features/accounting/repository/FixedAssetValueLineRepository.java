package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FixedAssetValueLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedAssetValueLineRepository extends JpaRepository<FixedAssetValueLine, Long> {

    List<FixedAssetValueLine> findAllByAssetIdOrderByValueDateAscIdAsc(Long assetId);
}
