package com.sni.bokaticowork.features.subscription.subscription.pass.repository;

import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassValidityRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PassValidityRuleRepository extends JpaRepository<PassValidityRule, Long> {

    /**
     * Regles applicables a un pass : les siennes propres, et celles de la version de plan dont il
     * est issu. Une requete unique · a la borne, chaque aller-retour se voit.
     */
    @Query("""
            SELECT r FROM PassValidityRule r
            WHERE r.pass.id = :passId
               OR (:passVersionId IS NOT NULL AND r.passVersion.id = :passVersionId)
            """)
    List<PassValidityRule> findApplicable(@Param("passId") Long passId,
                                          @Param("passVersionId") Long passVersionId);
}
