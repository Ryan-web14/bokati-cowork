package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccStatementLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccStatementLineRepository extends JpaRepository<AccStatementLine, Long> {
    List<AccStatementLine> findAllByTemplateIdOrderByPositionAsc(Long templateId);
}
