package com.sni.bokaticowork.features.document.template.repository;

import com.sni.bokaticowork.features.document.template.model.DocumentFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentFieldValueRepository extends JpaRepository<DocumentFieldValue, Long> {

    List<DocumentFieldValue> findByTemplateCodeAndSourceCode(String templateCode, String sourceCode);
}
