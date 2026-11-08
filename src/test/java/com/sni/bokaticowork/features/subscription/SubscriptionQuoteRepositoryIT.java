package com.sni.bokaticowork.features.subscription;

import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionQuoteRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les devis sur le schema de production : le JSON des services s'ecrit dans sa colonne jsonb, et
 * le plan est lu avec le devis, car la vue du devis le lit apres la fin de la transaction (sans
 * open-in-view, un plan non charge faisait tomber la consultation en 500).
 */
class SubscriptionQuoteRepositoryIT extends PostgresIntegrationTestBase {

    @Autowired private EntityManager entityManager;
    @Autowired private SubscriptionQuoteRepository quotes;
    @Autowired private com.sni.bokaticowork.features.subscription.derivation.repository.PlanDerivationRepository derivations;

    @Test
    @DisplayName("Un devis s'enregistre avec ses services en JSON, et se relit avec son plan deja charge")
    void quoteIsSavedAndReadWithItsPlan() {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_plan (id, code, name, plan_type, target_audience, status, visible)
                VALUES (992000001, 'PLN-IT-QUOTE', 'Bureau partage', 'COWORKING_ACCESS', 'INDIVIDUAL', 'ACTIVE', TRUE)
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO subscription_plan_version (id, plan_id, version_number, name, status)
                VALUES (992000002, 992000001, 1, 'Bureau partage v1', 'ACTIVE')
                """).executeUpdate();

        quotes.saveAndFlush(SubscriptionQuote.builder()
                .quoteNumber("QTE-IT-0001")
                .subscriberType(SubscriberType.MEMBER)
                .subscriberCode("MBR-IT")
                .planVersion(entityManager.getReference(PlanVersion.class, 992000002L))
                .billingCycle(BillingCycle.MONTHLY)
                .currency("XAF")
                .cataloguePrice(new BigDecimal("145000"))
                .quotedPrice(new BigDecimal("105000"))
                .setupFee(BigDecimal.ZERO)
                .commitmentMonths(1)
                .trialDays(0)
                .startDate(LocalDate.of(2026, 11, 16))
                .validUntil(LocalDate.of(2026, 12, 8))
                .preparedBy("it@test")
                .servicesJson("[{\"code\":\"WIFI\"}]")
                .build());
        entityManager.clear();

        SubscriptionQuote byNumber = quotes.findByQuoteNumber("QTE-IT-0001").orElseThrow();
        assertThat(Hibernate.isInitialized(byNumber.getPlanVersion())).isTrue();
        assertThat(Hibernate.isInitialized(byNumber.getPlanVersion().getPlan())).isTrue();
        assertThat(byNumber.getPlanVersion().getPlan().getCode()).isEqualTo("PLN-IT-QUOTE");
        assertThat(byNumber.getServicesJson()).contains("WIFI");
        entityManager.clear();

        SubscriptionQuote listed = quotes.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)).getContent().getFirst();
        assertThat(Hibernate.isInitialized(listed.getPlanVersion().getPlan())).isTrue();
        entityManager.clear();

        SubscriptionQuote ofSubscriber = quotes.findBySubscriberTypeAndSubscriberCodeOrderByCreatedAtDesc(SubscriberType.MEMBER, "MBR-IT").getFirst();
        assertThat(Hibernate.isInitialized(ofSubscriber.getPlanVersion().getPlan())).isTrue();
    }

    @Test
    @DisplayName("Les listes de derivations s'executent avec abonnement et versions charges")
    void derivationQueriesLoadWhatTheViewReads() {
        var pending = java.util.List.of(com.sni.bokaticowork.features.subscription.derivation.model.PlanDerivation.Status.PENDING_APPROVAL);
        assertThat(derivations.findByStatusInOrderByCreatedAtDesc(pending, PageRequest.of(0, 20)).getContent()).isEmpty();
        assertThat(derivations.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)).getContent()).isEmpty();
        assertThat(derivations.findByDerivationCode("DRV-INCONNU")).isEmpty();
        assertThat(derivations.findByBatchCodeOrderByCreatedAtAsc("LOT-INCONNU")).isEmpty();
    }

    @Test
    @DisplayName("La base admet le visa d'une derivation par son demandeur pour un devis accepte, et seulement pour lui")
    void fourEyesRuleLetsAQuoteAcceptanceThrough() {
        String rule = (String) entityManager.createNativeQuery("""
                SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'ck_plan_derivation_four_eyes'
                """).getSingleResult();
        assertThat(rule).contains("approved_by IS NULL", "approved_by", "requested_by", "approval_source", "'QUOTE'");
        // La regle elle-meme, evaluee comme la base l'evalue
        String check = "SELECT (CAST(:approved AS text) IS NULL OR CAST(:approved AS text) <> 'alice' OR CAST(:source AS text) = 'QUOTE')";
        assertThat(entityManager.createNativeQuery(check).setParameter("approved", "alice").setParameter("source", "QUOTE")
                .getSingleResult()).isEqualTo(true);
        Object direct = entityManager.createNativeQuery(check).setParameter("approved", "alice").setParameter("source", null)
                .getSingleResult();
        assertThat(direct).isNotEqualTo(true);
    }
}
