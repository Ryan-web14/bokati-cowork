package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDiscountType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot C10 : 25 000 de debours sur une facture de 100 000 HT. La TVA ne porte que sur les 100 000,
 * la remise globale de 10 % ne touche pas les debours, qui s'ajoutent au total a l'identique.
 */
class BillingDisbursementC10Test {

    private final BillingTaxRuleResolver resolver = mock(BillingTaxRuleResolver.class);
    private final ServiceCatalogItemRepository items = mock(ServiceCatalogItemRepository.class);
    private final BillingCalculationService service = new BillingCalculationService(resolver, items);

    @BeforeEach
    void setUp() {
        when(resolver.defaultTaxProfile()).thenReturn(new BillingTaxRuleResolver.TaxProfile(new BigDecimal("18"), BigDecimal.ZERO));
        when(items.findByItemCode(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void shouldKeepDisbursementsOutOfVatAndOutOfTheGlobalDiscount() {
        var result = service.calculate(List.of(line(BillingLineType.SERVICE, "100000"), line(BillingLineType.DISBURSEMENT, "25000")),
                List.of(new CreateBillingDocumentDiscountRequest(null, "Remise fidélité", BillingDiscountType.PERCENTAGE, new BigDecimal("10"),
                        null, null)));

        assertThat(result.lines().get(1).getTaxable()).isFalse();
        assertThat(result.lines().get(1).getVatAmount()).isEqualByComparingTo("0");
        // 100 000 HT + 18 000 TVA = 118 000, moins 10 % = 106 200 ; plus 25 000 de debours.
        assertThat(result.totalAmount()).isEqualByComparingTo("131200");
        assertThat(result.vatAmount()).isEqualByComparingTo("16200");
    }

    @Test
    void shouldNeverTaxADisbursementEvenIfTheScreenAsks() {
        var result = service.calculate(List.of(line(BillingLineType.DISBURSEMENT, "5000")), List.of());

        assertThat(result.taxAmount()).isEqualByComparingTo("0");
        assertThat(result.totalAmount()).isEqualByComparingTo("5000");
    }

    private static CreateBillingDocumentLineRequest line(BillingLineType type, String price) {
        return new CreateBillingDocumentLineRequest(null, type, null, type == BillingLineType.DISBURSEMENT ? "Frais de timbre" : "Bureau", null,
                BigDecimal.ONE, new BigDecimal(price), BigDecimal.ZERO, BigDecimal.ZERO, true, false, null, null, null, null, null, null, null,
                null, null);
    }
}
