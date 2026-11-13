package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.model.FaCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FaCategoryRepository extends JpaRepository<FaCategory, String> {

    List<FaCategory> findAllByOrderByCodeAsc();
}
