package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingCalculationService;
import com.sni.bokaticowork.features.billing.service.support.BillingCustomerSnapshotResolver;
import com.sni.bokaticowork.features.company.service.support.OrganizationBillingProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une organisation bloquee ne recoit plus de facture, et le plafond d'encours tient.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingOrganizationGuardsTest {

    @Mock private BillingCustomerSnapshotResolver customerSnapshotResolver;
    @Mock private BillingCalculationService calculationService;
    @Mock private BillingDocumentRepository documentRepository;
    @Mock private com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver billingCompanyResolver;
    @Mock private OrganizationBillingProfile organizationProfile;
    @InjectMocks private BillingDocumentServiceImpl service;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static OrganizationBillingProfile.Terms terms(boolean blocked, String creditLimit) {
        return new OrganizationBillingProfile.Terms("CUS-COM-1", "SARL Mboté", null, null, false, null, 30,
                creditLimit == null ? null : new BigDecimal(creditLimit), blocked, blocked ? "Impayés de septembre" : null,
                null, null);
    }

    private static CreateBillingDocumentRequest invoice() {
        CreateBillingDocumentLineRequest line = new CreateBillingDocumentLineRequest(1, null, null, "Bureau", null,
                BigDecimal.ONE, new BigDecimal("100000"), null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        return new CreateBillingDocumentRequest(BillingDocumentType.INVOICE, "CUSTOMER", "CUS-COM-1", null, null, null, null,
                null, null, null, null, null, "XAF", null, null, null, List.of(line), List.of(), List.of(),
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private void givenOrganization(OrganizationBillingProfile.Terms terms) {
        when(customerSnapshotResolver.resolve(any())).thenReturn(new BillingCustomerSnapshotResolver.CustomerSnapshot(
                "CUSTOMER", "CUS-COM-1", "SARL Mboté", null, null, null, terms));
        when(calculationService.calculate(any(), any())).thenReturn(new BillingCalculationService.CalculatedDocument(
                List.of(), new BigDecimal("100000"), BigDecimal.ZERO, List.of(), new BigDecimal("100000"),
                new BigDecimal("18000"), new BigDecimal("900"), new BigDecimal("18900"), new BigDecimal("118900")));
    }

    @Test
    @DisplayName("Organisation bloquee : la facture est refusee avec le motif")
    void blockedOrganizationIsRefused() {
        givenOrganization(terms(true, null));

        assertThatThrownBy(() -> service.create(invoice()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("bloquée")
                .hasMessageContaining("Impayés de septembre");
        verify(calculationService, never()).calculate(any(), any());
    }

    @Test
    @DisplayName("Plafond d'encours depasse : la facture est refusee")
    void creditLimitIsEnforced() {
        givenOrganization(terms(false, "200000"));
        when(documentRepository.sumCompanyOutstanding(eq("CUS-COM-1"), any(), any())).thenReturn(new BigDecimal("150000"));

        assertThatThrownBy(() -> service.create(invoice()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Plafond d'encours dépassé");
    }

    @Test
    @DisplayName("Un super-administrateur passe outre le plafond")
    void superAdminBypassesCreditLimit() {
        givenOrganization(terms(false, "200000"));
        when(documentRepository.sumCompanyOutstanding(anyString(), any(), any())).thenReturn(new BigDecimal("150000"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "chef", null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))));

        // Le plafond ne bloque plus · la suite (numerotation) n'est pas simulee ici et s'arrete plus loin.
        assertThatThrownBy(() -> service.create(invoice()))
                .isNotInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Facture de membre pour une organisation bloquee : refusee avec le motif")
    void memberInvoiceForBlockedCompanyIsRefused() {
        when(customerSnapshotResolver.resolve(any())).thenReturn(new BillingCustomerSnapshotResolver.CustomerSnapshot(
                "MEMBER", "MBR-1", "Jean Mbemba", null, null, null, null));
        com.sni.bokaticowork.features.client.customer.model.Customer company =
                com.sni.bokaticowork.features.client.customer.model.Customer.builder().customerId("CUS-COM-1").build();
        when(billingCompanyResolver.resolve("MEMBER", "MBR-1", null)).thenReturn(java.util.Optional.of(company));
        when(organizationProfile.of(company)).thenReturn(java.util.Optional.of(terms(true, null)));

        assertThatThrownBy(() -> service.create(invoice()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Impayés de septembre");
    }
}
