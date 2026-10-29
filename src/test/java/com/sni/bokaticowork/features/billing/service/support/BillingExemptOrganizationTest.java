package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Une organisation exoneree paie le prix hors taxe · jamais le TTC rebaptise HT.
 */
class BillingExemptOrganizationTest {

    private final BillingTaxRuleResolver taxRuleResolver = mock(BillingTaxRuleResolver.class);
    private final ServiceCatalogItemRepository catalogItemRepository = mock(ServiceCatalogItemRepository.class);
    private final BillingCalculationService service = new BillingCalculationService(taxRuleResolver, catalogItemRepository);

    @BeforeEach
    void setUp() {
        // TVA 18 %, centime additionnel 5 % de la TVA · TTC = HT x 1,189.
        when(taxRuleResolver.defaultTaxProfile()).thenReturn(new BillingTaxRuleResolver.TaxProfile(
                new BigDecimal("18"), new BigDecimal("5")));
        when(catalogItemRepository.findByItemCode(anyString())).thenReturn(Optional.empty());
    }

    private static CreateBillingDocumentLineRequest line(String unitPrice, Boolean taxable, Boolean taxIncluded) {
        return new CreateBillingDocumentLineRequest(1, null, null, "Bureau privé", null, BigDecimal.ONE,
                new BigDecimal(unitPrice), null, null, taxable, taxIncluded, null, null,
                null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Un prix TTC est ramene au HT, puis facture sans taxe")
    void taxIncludedPriceDropsToNet() {
        var calculated = service.calculate(service.exempted(List.of(line("118900", true, true))), List.of());

        assertThat(calculated.taxAmount()).isEqualByComparingTo("0");
        assertThat(calculated.totalAmount()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Un prix HT reste tel quel, sans TVA ni centime")
    void netPriceStays() {
        var calculated = service.calculate(service.exempted(List.of(line("100000", null, null))), List.of());

        assertThat(calculated.vatAmount()).isEqualByComparingTo("0");
        assertThat(calculated.additionalCentAmount()).isEqualByComparingTo("0");
        assertThat(calculated.totalAmount()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Sans exoneration, le meme prix HT porte sa TVA")
    void subjectOrganizationPaysVat() {
        var calculated = service.calculate(List.of(line("100000", null, null)), List.of());

        assertThat(calculated.totalAmount()).isEqualByComparingTo("118900");
    }
}
