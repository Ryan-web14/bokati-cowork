package com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.repository;

import com.sni.bokaticowork.features.subscription.promotion.pricing.pricelist.model.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    @Query("SELECT p FROM PriceList p WHERE lower(p.code) = lower(:code)")
    Optional<PriceList> findByCode(@Param("code") String code);

    /**
     * Grilles susceptibles de s'appliquer a ce souscripteur : celles destinees a tous, et celles
     * qui le designent lui, son entreprise, son segment ou son partenaire.
     *
     * <p>La requete ne tranche pas entre elles · elle rend les candidates, et le choix de celle qui
     * gagne appartient au resolveur, qui connait la regle de priorite et de specificite.</p>
     */
    @Query(nativeQuery = true, value = """
            SELECT *
            FROM price_list
            WHERE active = TRUE
              AND currency = CAST(:currency AS VARCHAR)
              AND (valid_from IS NULL OR valid_from <= :now)
              AND (valid_until IS NULL OR valid_until >= :now)
              AND (
                    audience_type = 'ALL'
                    OR (audience_type = 'SUBSCRIBER' AND audience_code = CAST(:subscriberCode AS VARCHAR))
                    OR (audience_type = 'SEGMENT' AND audience_code = CAST(:segment AS VARCHAR))
                    OR (audience_type = 'BUSINESS_ENTITY' AND audience_code = CAST(:businessCode AS VARCHAR))
                    OR (audience_type = 'PARTNER' AND audience_code = CAST(:partnerCode AS VARCHAR))
                  )
            ORDER BY priority ASC, id ASC
            """)
    List<PriceList> findCandidates(@Param("currency") String currency,
                                   @Param("subscriberCode") String subscriberCode,
                                   @Param("segment") String segment,
                                   @Param("businessCode") String businessCode,
                                   @Param("partnerCode") String partnerCode,
                                   @Param("now") Instant now);
}
