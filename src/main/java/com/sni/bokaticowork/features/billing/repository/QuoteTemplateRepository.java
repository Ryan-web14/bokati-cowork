package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.QuoteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C14 : modeles de devis. */
@Repository
public interface QuoteTemplateRepository extends JpaRepository<QuoteTemplate, Long> {

    Optional<QuoteTemplate> findByCode(String code);

    List<QuoteTemplate> findAllByOrderByNameAsc();
}
