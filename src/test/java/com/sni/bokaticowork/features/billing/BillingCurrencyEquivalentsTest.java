package com.sni.bokaticowork.features.billing;

import com.sni.bokaticowork.features.accounting.service.implementation.ExchangeRateService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.service.support.BillingCurrencyEquivalents;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Montants des factures en USD et en EUR. */
class BillingCurrencyEquivalentsTest {

    private BillingCurrencyEquivalents equivalents() {
        ExchangeRateService rates = mock(ExchangeRateService.class);
        // 1 EUR = 655,957 XAF ; 1 USD = 600 XAF.
        when(rates.convert(any(), eq("XAF"), eq("EUR"), any())).thenAnswer(i -> ((BigDecimal) i.getArgument(0))
                .divide(new BigDecimal("655.957"), 4, RoundingMode.HALF_UP));
        when(rates.convert(any(), eq("XAF"), eq("USD"), any())).thenAnswer(i -> ((BigDecimal) i.getArgument(0))
                .divide(new BigDecimal("600"), 4, RoundingMode.HALF_UP));
        when(rates.convert(eq(BigDecimal.ONE), eq("EUR"), eq("XAF"), any())).thenReturn(new BigDecimal("655.957"));
        when(rates.convert(eq(BigDecimal.ONE), eq("USD"), eq("XAF"), any())).thenReturn(new BigDecimal("600"));
        BillingCurrencyEquivalents service = new BillingCurrencyEquivalents();
        ReflectionTestUtils.setField(service, "rates", rates);
        return service;
    }

    @Test
    void anInvoiceShowsItsAmountInUsdAndEur() {
        List<BillingCurrencyEquivalents.Equivalent> result = equivalents()
                .compute(new BigDecimal("655957"), new BigDecimal("327978.5"), "XAF", LocalDate.of(2026, 11, 18), false);

        assertThat(result).extracting(BillingCurrencyEquivalents.Equivalent::currency).containsExactly("USD", "EUR");
        assertThat(result.get(1).totalAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.get(1).balanceDue()).isEqualByComparingTo("500.00");
        assertThat(result.get(0).totalAmount()).isEqualByComparingTo("1093.26");
        assertThat(result.get(0).rate()).isEqualByComparingTo("600");
    }

    @Test
    void equivalentsAreFrozenAtIssue() {
        BillingCurrencyEquivalents service = equivalents();
        BillingDocument document = new BillingDocument();
        document.setDocumentNumber("FAC-1");
        document.setCurrency("XAF");
        document.setTotalAmount(new BigDecimal("655957"));
        document.setBalanceDue(new BigDecimal("655957"));
        document.setIssueDate(LocalDate.of(2026, 11, 18));

        service.freeze(document);
        assertThat(document.getCurrencyEquivalentsJson()).contains("EUR").contains("USD");

        // Paiement partiel : le reste du suit, au taux fige.
        document.setBalanceDue(new BigDecimal("327978.5"));
        ReflectionTestUtils.setField(service, "rates", null);
        List<BillingCurrencyEquivalents.Equivalent> frozen = service.forDocument(document);
        assertThat(frozen).allMatch(BillingCurrencyEquivalents.Equivalent::frozen);
        assertThat(frozen.stream().filter(e -> e.currency().equals("EUR")).findFirst().orElseThrow().balanceDue())
                .isEqualByComparingTo("500.00");
    }
}
