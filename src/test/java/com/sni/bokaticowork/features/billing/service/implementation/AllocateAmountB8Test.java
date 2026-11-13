package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.config.BillingDiscountGuardProperties;
import com.sni.bokaticowork.features.billing.dto.request.AllocateAmountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.response.AllocateAmountResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingCalculationService;
import com.sni.bokaticowork.features.billing.service.support.BillingCustomerSnapshotResolver;
import com.sni.bokaticowork.features.billing.service.support.BillingDiscountGuard;
import com.sni.bokaticowork.features.billing.service.support.BillingTaxRuleResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot B8 : 500 000 TTC saisis sur trois lignes a 18 % de TVA (et centimes additionnels) donnent des
 * HT et des taxes par ligne dont la somme fait exactement 500 000, avec la remise implicite affichee.
 */
class AllocateAmountB8Test {

    private final BillingTaxRuleResolver taxRules = mock(BillingTaxRuleResolver.class);
    private final ServiceCatalogItemRepository catalog = mock(ServiceCatalogItemRepository.class);
    private final BillingCustomerSnapshotResolver customers = mock(BillingCustomerSnapshotResolver.class);
    private final BillingDocumentServiceImpl service = mock(BillingDocumentServiceImpl.class, org.mockito.Mockito.CALLS_REAL_METHODS);

    @BeforeEach
    void setUp() {
        when(taxRules.defaultTaxProfile()).thenReturn(new BillingTaxRuleResolver.TaxProfile(new BigDecimal("18"), new BigDecimal("5")));
        when(catalog.findByItemCode(anyString())).thenReturn(Optional.empty());
        when(customers.organizationOf(any(), any())).thenReturn(Optional.empty());
        ReflectionTestUtils.setField(service, "calculationService", new BillingCalculationService(taxRules, catalog));
        ReflectionTestUtils.setField(service, "discountGuard", new BillingDiscountGuard(catalog, new BillingDiscountGuardProperties()));
        ReflectionTestUtils.setField(service, "customerSnapshotResolver", customers);
        ReflectionTestUtils.setField(service, "defaultCurrency", "XAF");
    }

    @Test
    void shouldSplitATaxIncludedTotalToTheFranc() {
        AllocateAmountResponse result = service.allocateAmount(new AllocateAmountRequest(List.of(
                line("Bureau privatif", "2", "150000"), line("Salle de réunion", "3", "40000"), line("Café", "10", "2500")),
                new BigDecimal("500000"), true, null, BillingDocumentType.QUOTE, "XAF", null, null, null));

        assertThat(result.simulation().totalAmount()).isEqualByComparingTo("500000");
        assertThat(result.simulation().lines()).allSatisfy(line -> assertThat(line.totalAmount().stripTrailingZeros().scale()).isLessThanOrEqualTo(0));
        // Reference : 445 000 HT, soit 549 914,50 TTC ; la remise implicite est l'ecart.
        assertThat(result.referenceAmount()).isGreaterThan(new BigDecimal("500000"));
        assertThat(result.implicitDiscount()).isEqualByComparingTo(result.referenceAmount().subtract(new BigDecimal("500000")));
        BigDecimal taxable = result.simulation().taxableAmount();
        BigDecimal taxes = result.simulation().taxAmount();
        assertThat(taxable.add(taxes)).isEqualByComparingTo("500000");
        assertThat(result.lines()).allSatisfy(line -> assertThat(line.taxIncluded()).isTrue());
    }

    @Test
    void shouldSplitAnExcludingTaxTotalOverChosenLinesOnly() {
        AllocateAmountResponse result = service.allocateAmount(new AllocateAmountRequest(List.of(
                line("Bureau privatif", "1", "300000"), line("Salle", "1", "100000"), line("Café", "1", "5000")),
                new BigDecimal("350000"), false, List.of(0, 1), null, null, null, null, null));

        assertThat(result.simulation().lines().get(0).netAmount().add(result.simulation().lines().get(1).netAmount()))
                .isEqualByComparingTo("350000");
        // La troisieme ligne garde son prix.
        assertThat(result.lines().get(2).unitPrice()).isEqualByComparingTo("5000");
        assertThat(result.lines().get(2).discountAmount()).isNull();
    }

    private static CreateBillingDocumentLineRequest line(String description, String quantity, String unitPrice) {
        return new CreateBillingDocumentLineRequest(null, BillingLineType.SERVICE, null, description, null, new BigDecimal(quantity),
                new BigDecimal(unitPrice), null, null, true, false, null, null, null, null, null, null, null, false, null);
    }
}
