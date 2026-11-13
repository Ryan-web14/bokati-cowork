package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccNoteText;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccNoteTextRepository extends JpaRepository<AccNoteText, Long> {
    List<AccNoteText> findAllByFiscalYearCode(String fiscalYearCode);
    java.util.Optional<AccNoteText> findByFiscalYearCodeAndNoteCode(String fiscalYearCode, String noteCode);
}
