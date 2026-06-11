package com.sni.bokaticowork.features.support.repository;

import com.sni.bokaticowork.features.support.model.SupportTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportTagRepository extends JpaRepository<SupportTag, Long> {
    Optional<SupportTag> findByNameIgnoreCase(String name);

    List<SupportTag> findAllByActiveTrueOrderByNameAsc();
}
