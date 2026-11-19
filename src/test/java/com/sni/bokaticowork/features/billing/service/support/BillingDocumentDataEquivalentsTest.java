package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.implementation.BillingDocumentPdfServiceImpl.BillingDocumentTemplateFormatter;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Le modele en blocs de la facture imprime aussi les montants en USD et en EUR, comme le gabarit d'origine. */
class BillingDocumentDataEquivalentsTest {

    @Test
    void theInvoiceTemplateDataCarriesTheForeignCurrencyTotals() {
        BillingDocumentResponse document = mock(BillingDocumentResponse.class);
        when(document.documentNumber()).thenReturn("INV-1");
        when(document.currency()).thenReturn("XAF");
        BillingDocumentTemplateFormatter fmt = mock(BillingDocumentTemplateFormatter.class);
        List<BillingCurrencyEquivalents.Equivalent> equivalents = List.of(
                new BillingCurrencyEquivalents.Equivalent("USD", new BigDecimal("615.5"), new BigDecimal("958.5703"), BigDecimal.ZERO,
                        LocalDate.of(2026, 11, 18), true),
                new BillingCurrencyEquivalents.Equivalent("EUR", new BigDecimal("655.957"), new BigDecimal("1899.45"), BigDecimal.ZERO,
                        LocalDate.of(2026, 11, 18), true));

        DocumentData data = BillingDocumentData.from(document, fmt, null, null, equivalents);

        assertThat(data.value("equivalent_usd")).isEqualTo("958,57 USD");
        assertThat(data.label("equivalent_usd")).isEqualTo("Soit en USD (1 USD = 615,50 XAF, taux du 18/11/2026)");
        assertThat(data.value("equivalent_eur")).isEqualTo("1 899,45 EUR");
        assertThat(data.sections()).filteredOn(section -> "Totaux".equals(section.title())).singleElement()
                .satisfies(section -> assertThat(section.fieldKeys()).endsWith("reste_du", "equivalent_usd", "equivalent_eur"));
    }

    @Test
    void withoutEquivalentsTheTotalsAreUnchangedAndTheSampleShowsThem() {
        BillingDocumentResponse document = mock(BillingDocumentResponse.class);
        DocumentData data = BillingDocumentData.from(document, mock(BillingDocumentTemplateFormatter.class), null, null);
        assertThat(data.fields()).doesNotContainKey("equivalent_usd");

        DocumentData sample = BillingDocumentData.sample(BillingDocumentData.INVOICE);
        assertThat(sample.value("equivalent_usd")).isEqualTo("958,57 USD");
        assertThat(sample.value("equivalent_eur")).isEqualTo("899,45 EUR");
    }
}
