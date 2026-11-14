package com.sni.bokaticowork.features.document.template.repository;

import com.sni.bokaticowork.features.document.template.model.DocumentTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DocumentTemplateVersionRepository extends JpaRepository<DocumentTemplateVersion, Long> {

    Optional<DocumentTemplateVersion> findFirstByTemplateCodeAndStatus(String templateCode, String status);

    Optional<DocumentTemplateVersion> findByTemplateCodeAndVersionNumber(String templateCode, Integer versionNumber);

    List<DocumentTemplateVersion> findByTemplateCodeOrderByVersionNumberDesc(String templateCode);

    List<DocumentTemplateVersion> findByStatus(String status);

    @Query("select coalesce(max(v.versionNumber), 0) from DocumentTemplateVersion v where v.templateCode = :code")
    int maxVersion(String code);
}
