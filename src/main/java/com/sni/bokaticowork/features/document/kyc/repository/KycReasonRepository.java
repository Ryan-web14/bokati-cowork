package com.sni.bokaticowork.features.document.kyc.repository;

import com.sni.bokaticowork.features.document.kyc.model.KycReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KycReasonRepository extends JpaRepository<KycReason, Long> {

    Optional<KycReason> findByCode(String code);

    List<KycReason> findAllByOrderBySortOrderAscLabelAsc();
}
