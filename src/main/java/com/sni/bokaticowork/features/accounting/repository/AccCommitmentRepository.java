package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccCommitment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccCommitmentRepository extends JpaRepository<AccCommitment, Long> {

    java.util.Optional<AccCommitment> findByCode(String code);

    java.util.List<AccCommitment> findAllByOrderByCodeAsc();

    java.util.List<AccCommitment> findAllByStatus(String status);

    java.util.List<AccCommitment> findAllByLinkedTypeAndLinkedCode(String linkedType, String linkedCode);
}
