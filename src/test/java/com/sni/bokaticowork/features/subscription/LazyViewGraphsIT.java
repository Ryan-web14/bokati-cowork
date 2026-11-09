package com.sni.bokaticowork.features.subscription;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationRegistration;
import com.sni.bokaticowork.features.domiciliation.model.MailItem;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationAddressRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationContractRepository;
import com.sni.bokaticowork.features.domiciliation.repository.DomiciliationRegistrationRepository;
import com.sni.bokaticowork.features.domiciliation.repository.MailItemRepository;
import com.sni.bokaticowork.features.domiciliation.repository.SubscriptionServiceRepository;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionDebitMandateRepository;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionTerminationRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponRepository;
import com.sni.bokaticowork.features.subscription.repository.PassEventRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRenewalScheduleRepository;
import com.sni.bokaticowork.features.subscription.repository.PassStatusHistoryRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassEvent;
import com.sni.bokaticowork.features.subscription.subscription.model.PassStatusHistory;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ce que les vues lisent apres la fin de la transaction (open-in-view coupe) est charge par les
 * requetes elles-memes : chaque graphe pose ici s'execute, ce qui valide ses noms d'attributs.
 */
class LazyViewGraphsIT extends PostgresIntegrationTestBase {

    @Autowired private EntityManager entityManager;
    @Autowired private SubscriptionDebitMandateRepository mandates;
    @Autowired private SubscriptionTerminationRepository terminations;
    @Autowired private PassEventRepository passEvents;
    @Autowired private PassStatusHistoryRepository passHistory;
    @Autowired private PassRenewalScheduleRepository passRenewals;
    @Autowired private CouponRepository coupons;
    @Autowired private CustomerMemberLinkRepository memberLinks;
    @Autowired private DomiciliationContractRepository domiciliationContracts;
    @Autowired private MailItemRepository mailItems;
    @Autowired private DomiciliationRegistrationRepository registrations;
    @Autowired private SubscriptionServiceRepository subscribedServices;
    @Autowired private DomiciliationAddressRepository domiciliationAddresses;

    @Test
    @DisplayName("Les requetes des vues d'abonnement s'executent avec leurs graphes")
    void subscriptionGraphsExecute() {
        assertThat(mandates.findByMandateCode("MDT-INCONNU")).isEmpty();
        assertThat(terminations.findByTerminationCode("TRM-INCONNU")).isEmpty();
        assertThat(terminations.findBySubscription_IdOrderByRequestedAtDesc(-1L)).isEmpty();
        assertThat(terminations.findByStatusOrderByEffectiveDateAsc(SubscriptionTermination.Status.REQUESTED)).isEmpty();
        assertThat(coupons.findByCode("CPN-INCONNU")).isEmpty();
        assertThat(passRenewals.findByPassId(-1L)).isEmpty();
    }

    @Test
    @DisplayName("Les requetes des vues de domiciliation et des liens d'organisation s'executent avec leurs graphes")
    void domiciliationGraphsExecute() {
        var page = PageRequest.of(0, 20);
        assertThat(domiciliationContracts.findByContractNumber("DOM-INCONNU")).isEmpty();
        assertThat(domiciliationContracts.findAllByOrderByCreatedAtDesc(page).getContent()).isEmpty();
        assertThat(domiciliationContracts.findByStatusInOrderByCreatedAtDesc(List.of(DomiciliationContract.Status.values()), page).getContent()).isEmpty();
        assertThat(domiciliationContracts.findBySubscription_SubscriberTypeAndSubscription_SubscriberCodeOrderByCreatedAtDesc(
                SubscriberType.CUSTOMER, "CUS-INCONNU")).isEmpty();
        assertThat(mailItems.findByItemNumber("PLI-INCONNU")).isEmpty();
        assertThat(mailItems.findByContract_IdOrderByReceivedAtDesc(-1L, page).getContent()).isEmpty();
        assertThat(mailItems.findByStatusInOrderByReceivedAtAsc(List.of(MailItem.Status.values()), page).getContent()).isEmpty();
        assertThat(registrations.findByStatusInOrderBySubmittedAtAsc(List.of(DomiciliationRegistration.Status.values()), page).getContent()).isEmpty();
        assertThat(subscribedServices.findByServiceNumber("SVC-INCONNU")).isEmpty();
        assertThat(subscribedServices.findBySubscription_IdOrderByCreatedAtAsc(-1L)).isEmpty();
        assertThat(domiciliationAddresses.findByCode("ADR-INCONNU")).isEmpty();
        assertThat(domiciliationAddresses.findAllByOrderByLabelAsc()).isNotNull();
        Member nobody = entityManager.getReference(Member.class, -1L);
        assertThat(memberLinks.findAllByMemberAndStatusOrderByStartedAtDesc(nobody, MemberLinkStatus.ACTIVE)).isEmpty();
    }

    @Test
    @DisplayName("Evenements et historique d'un pass se relisent avec leur pass, et se serialisent sans ses liens paresseux")
    void passEventsComeWithTheirPassAndSerialize() {
        Pass pass = Pass.builder().passNumber("PAS-IT-LAZY").passType(PassType.DAY_PASS).ownerType(SubscriberType.MEMBER)
                .ownerCode("MBR-IT").name("Pass journee").validFrom(Instant.parse("2026-11-01T08:00:00Z")).build();
        entityManager.persist(pass);
        entityManager.persist(PassEvent.builder().pass(pass).eventType("PASS_CREATED").occurredAt(Instant.now()).build());
        entityManager.persist(PassStatusHistory.builder().pass(pass).toStatus("ACTIVE").changedAt(Instant.now()).build());
        entityManager.flush();
        entityManager.clear();

        // Le controleur relit le pass dans une autre transaction : il arrive detache
        Pass detached = entityManager.find(Pass.class, pass.getId());
        entityManager.clear();

        List<PassEvent> events = passEvents.findAllByPassOrderByOccurredAtDesc(detached);
        assertThat(events).hasSize(1);
        assertThat(Hibernate.isInitialized(events.getFirst().getPass())).isTrue();
        List<PassStatusHistory> history = passHistory.findAllByPassOrderByChangedAtDesc(detached);
        assertThat(history).hasSize(1);
        assertThat(Hibernate.isInitialized(history.getFirst().getPass())).isTrue();
        entityManager.clear();

        String json = JsonMapper.builder().build().writeValueAsString(events.getFirst());
        assertThat(json).contains("\"passNumber\":\"PAS-IT-LAZY\"").doesNotContain("\"subscription\"");
        assertThat(JsonMapper.builder().build().writeValueAsString(history.getFirst())).contains("PAS-IT-LAZY");
    }
}
