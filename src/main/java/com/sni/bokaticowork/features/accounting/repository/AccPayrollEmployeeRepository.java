package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccPayrollEmployee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccPayrollEmployeeRepository extends JpaRepository<AccPayrollEmployee, String> {

    java.util.List<AccPayrollEmployee> findAllByOrderByMatriculeAsc();
}
