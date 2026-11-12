package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPayrollRubric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPayrollRubricRepository extends JpaRepository<AccPayrollRubric, String> {

    java.util.List<AccPayrollRubric> findAllByOrderByCodeAsc();
}
