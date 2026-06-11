package com.sni.bokaticowork.features.document.documentMaster.repository;

import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLog, Long> {

    Page<DocumentAccessLog> findAllByDocumentOrderByAccessedAtDesc(Document document, Pageable pageable);

    Page<DocumentAccessLog> findAllByUserIdOrderByAccessedAtDesc(Long userId, Pageable pageable);
}
