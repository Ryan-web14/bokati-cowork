package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Les requetes de l'outil admin mobile money tiennent sur le schema de production. */
class MobileMoneyOperationsQueriesIT extends PostgresIntegrationTestBase {

    @Autowired private PawapayDepositRepository repository;
    @Autowired private PaymentTransactionRepository transactionRepository;

    @Test
    @DisplayName("Synthese, periode et recherche filtree s'executent")
    void queriesRun() {
        Instant from = Instant.now().minusSeconds(86_400);
        Instant to = Instant.now();

        assertThat(repository.summarize(from, to)).isNotNull();
        assertThat(repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(from, to,
                PageRequest.of(0, 10))).isNotNull();
        Specification<PawapayDeposit> byReference = (root, query, cb) -> cb.or(
                cb.equal(root.get("depositId"), "x"), cb.equal(root.get("providerTransactionId"), "x"),
                cb.like(root.get("phoneNumber"), "%242%"));
        assertThat(repository.findAll(byReference, PageRequest.of(0, 10))).isNotNull();
        assertThat(transactionRepository.findPendingMobileMoneyRefunds(Instant.now(), 10)).isNotNull();
    }
}
