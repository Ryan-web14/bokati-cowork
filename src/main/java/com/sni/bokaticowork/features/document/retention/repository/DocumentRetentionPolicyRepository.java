package com.sni.bokaticowork.features.document.retention.repository;

import com.sni.bokaticowork.features.document.retention.model.DocumentRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRetentionPolicyRepository extends JpaRepository<DocumentRetentionPolicy, Long> {

    List<DocumentRetentionPolicy> findAllByActiveTrue();

    Optional<DocumentRetentionPolicy> findByCode(String code);

    boolean existsByCode(String code);
}
