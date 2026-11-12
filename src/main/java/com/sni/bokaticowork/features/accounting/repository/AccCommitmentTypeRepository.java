package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.AccCommitmentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccCommitmentTypeRepository extends JpaRepository<AccCommitmentType, String> {

    java.util.List<AccCommitmentType> findAllByOrderByCodeAsc();
}
