package com.sni.bokaticowork.features.ressource.repository.repo;

import com.sni.bokaticowork.features.ressource.model.ResourceBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot R5c : lots de ressources. */
@Repository
public interface ResourceBundleRepository extends JpaRepository<ResourceBundle, Long> {

    Optional<ResourceBundle> findByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    List<ResourceBundle> findAllByOrderByNameAsc();
}
