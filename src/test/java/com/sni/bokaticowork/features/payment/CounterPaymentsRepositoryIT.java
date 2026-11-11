package com.sni.bokaticowork.features.payment;

import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentIntentRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Encaissement au guichet sur le schema de production : monnaie rendue, details d'un paiement hors
 * plateforme, rattachement a la session, annulation et relance d'une intention.
 */
class CounterPaymentsRepositoryIT extends PostgresIntegrationTestBase {

    @Autowired private EntityManager entityManager;
    @Autowired private PaymentTransactionRepository transactions;
    @Autowired private PaymentIntentRepository intents;

    @BeforeEach
    void seed() {
        entityManager.createNativeQuery("""
                INSERT INTO payment_intent (id, intent_number, customer_type, customer_code, amount, currency, status,
                                            cancelled_by, cancellation_reason, relaunch_count)
                VALUES (995000001, 'INT-IT-1', 'MEMBER', 'MBR-IT-1', 60000, 'XAF', 'CANCELLED', 'caisse@sni-cg.com', 'Client parti', 2)
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO payment_transaction (id, transaction_number, payment_intent_id, payment_method, provider,
                                                 provider_reference, amount, currency, status, tendered_amount,
                                                 change_amount, cash_session_number, bank_name, payer_name, payer_phone)
                VALUES (995000011, 'TXN-IT-CASH', 995000001, 'CASH', 'CASH', 'PREF-1', 17500, 'XAF', 'SUCCEEDED',
                        20000, 2500, 'CSS-IT-1', NULL, NULL, NULL),
                       (995000012, 'TXN-IT-CHQ', 995000001, 'CHEQUE', 'CHEQUE', '0012345', 30000, 'XAF', 'SUCCEEDED',
                        NULL, NULL, 'CSS-IT-1', 'BGFI', 'SARL Alpha', NULL),
                       (995000013, 'TXN-IT-MOMO', 995000001, 'MOBILE_MONEY_OFFLINE', 'MTN_MOMO', 'MP2611.1234', 12500, 'XAF',
                        'SUCCEEDED', NULL, NULL, 'CSS-IT-1', NULL, 'Grace M.', '+242061234567'),
                       (995000014, 'TXN-IT-FAIL', 995000001, 'CHEQUE', 'CHEQUE', '0099999', 5000, 'XAF', 'FAILED',
                        NULL, NULL, 'CSS-IT-1', 'BGFI', NULL, NULL)
                """).executeUpdate();
        entityManager.clear();
    }

    @Test
    @DisplayName("Les nouvelles colonnes se relisent sur la transaction et l'intention")
    void columnsAreMapped() {
        PaymentTransaction cash = transactions.findByTransactionNumber("TXN-IT-CASH").orElseThrow();
        assertThat(cash.getTenderedAmount()).isEqualByComparingTo("20000");
        assertThat(cash.getChangeAmount()).isEqualByComparingTo("2500");
        assertThat(cash.getCashSessionNumber()).isEqualTo("CSS-IT-1");

        PaymentTransaction momo = transactions.findByTransactionNumber("TXN-IT-MOMO").orElseThrow();
        assertThat(momo.getPaymentMethod()).isEqualTo(PaymentMethod.MOBILE_MONEY_OFFLINE);
        assertThat(momo.getPayerPhone()).isEqualTo("+242061234567");

        PaymentIntent intent = intents.findByIntentNumber("INT-IT-1").orElseThrow();
        assertThat(intent.getCancelledBy()).isEqualTo("caisse@sni-cg.com");
        assertThat(intent.getRelaunchCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Doublon de reference : la recherche retrouve le cheque, sans tenir compte de la casse")
    void duplicateReferenceLookupRuns() {
        assertThat(transactions.findAllByPaymentMethodAndProviderAndProviderReferenceIgnoreCaseAndStatus(
                PaymentMethod.MOBILE_MONEY_OFFLINE, "MTN_MOMO", "mp2611.1234", PaymentTransactionStatus.SUCCEEDED))
                .extracting(PaymentTransaction::getTransactionNumber)
                .containsExactly("TXN-IT-MOMO");
        assertThat(transactions.findAllByPaymentMethodAndProviderAndProviderReferenceIgnoreCaseAndStatus(
                PaymentMethod.CHEQUE, "CHEQUE", "0099999", PaymentTransactionStatus.SUCCEEDED))
                .as("un cheque en echec ne bloque pas une nouvelle saisie")
                .isEmpty();
    }

    @Test
    @DisplayName("Totaux hors especes d'une session : par moyen et operateur, especes et echecs exclus")
    void nonCashTotalsOfASession() {
        List<Object[]> rows = transactions.nonCashTotalsBySession("CSS-IT-1");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)[0]).isEqualTo(PaymentMethod.CHEQUE);
        assertThat(new BigDecimal(rows.get(0)[3].toString())).isEqualByComparingTo("30000");
        assertThat(rows.get(1)[0]).isEqualTo(PaymentMethod.MOBILE_MONEY_OFFLINE);
        assertThat(rows.get(1)[1]).isEqualTo("MTN_MOMO");
        assertThat(((Number) rows.get(1)[2]).longValue()).isEqualTo(1L);
        assertThat(new BigDecimal(rows.get(1)[3].toString())).isEqualByComparingTo("12500");
    }
}
