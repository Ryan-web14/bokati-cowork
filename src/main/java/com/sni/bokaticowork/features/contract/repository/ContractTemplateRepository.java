package com.sni.bokaticowork.features.contract.repository;

import com.sni.bokaticowork.features.contract.model.ContractTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, Long> {
    Optional<ContractTemplate> findByCode(String code);
    List<ContractTemplate> findAllByActiveTrueOrderByNameAsc();
    boolean existsByCode(String code);
    Page<ContractTemplate> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT t FROM ContractTemplate t WHERE " +
            "LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.category) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ContractTemplate> search(String query, Pageable pageable);

    Page<ContractTemplate> findAllByCategoryIgnoreCase(String category, Pageable pageable);
}
