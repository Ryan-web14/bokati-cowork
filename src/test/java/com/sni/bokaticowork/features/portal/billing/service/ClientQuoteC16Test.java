package com.sni.bokaticowork.features.portal.billing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dto.ClaimDtos;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.implementation.ClaimService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C16 : un membre accepte son devis depuis son espace ; il ne voit ni les pieces d'un autre ni
 * un brouillon ; il ouvre une reclamation par l'espace client (canal PORTAL) et ne retire que les siennes.
 */
class ClientQuoteC16Test {

    private final BillingDocumentService billing = mock(BillingDocumentService.class);
    private final ClaimService claims = mock(ClaimService.class);
    private final ClientBillingService clientBilling = mock(ClientBillingService.class);
    private final ClientQuoteService service = new ClientQuoteService(billing, mock(BillingDocumentPdfService.class), mock(PaymentService.class),
            claims, clientBilling);
    private final Member awa = new Member();

    @BeforeEach
    void setUp() {
        awa.setMemberId("MBR-1");
        // Les pieces simulees se preparent avant d'etre rendues (Mockito refuse une preparation imbriquee).
        BillingDocumentResponse dev1 = document("DEV-1", BillingDocumentType.QUOTE, BillingDocumentStatus.SENT, "MBR-1");
        BillingDocumentResponse dev2 = document("DEV-2", BillingDocumentType.QUOTE, BillingDocumentStatus.SENT, "MBR-2");
        BillingDocumentResponse dev3 = document("DEV-3", BillingDocumentType.QUOTE, BillingDocumentStatus.DRAFT, "MBR-1");
        BillingDocumentResponse dev4 = document("DEV-4", BillingDocumentType.QUOTE, BillingDocumentStatus.CONVERTED, "MBR-1");
        BillingDocumentResponse fac1 = document("FAC-1", BillingDocumentType.INVOICE, BillingDocumentStatus.ISSUED, "MBR-1");
        when(billing.get("DEV-1")).thenReturn(dev1);
        when(billing.get("DEV-2")).thenReturn(dev2);
        when(billing.get("DEV-3")).thenReturn(dev3);
        when(billing.get("DEV-4")).thenReturn(dev4);
        when(billing.get("FAC-1")).thenReturn(fac1);
    }

    @Test
    void shouldAcceptOwnQuoteOnly() {
        service.accept(awa, "DEV-1");
        verify(billing).acceptQuote("DEV-1");

        assertThatThrownBy(() -> service.accept(awa, "DEV-2")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.accept(awa, "DEV-3")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.accept(awa, "DEV-4")).isInstanceOf(BadRequestException.class);
        verify(billing, never()).acceptQuote("DEV-2");
    }

    @Test
    void shouldOpenAClaimFromThePortalAndWithdrawOnlyOwnClaims() {
        ClaimDtos.OpenRequest request = new ClaimDtos.OpenRequest("ERREUR_PRIX", "Prix du contrat", List.of(), null);
        service.openClaim(awa, "FAC-1", request);
        verify(claims).open(eq("FAC-1"), any(), eq("PORTAL"), eq("MBR-1"));

        when(claims.get("RCL-9")).thenReturn(new ClaimDtos.ClaimView("RCL-9", "FAC-9", "MEMBER", "MBR-2", "AUTRE", "Autre", null, List.of(),
                null, "OPEN", "PORTAL", "MBR-2", null, null, null, null, null, null));
        assertThatThrownBy(() -> service.withdrawClaim(awa, "RCL-9")).isInstanceOf(ResourceNotFoundException.class);
        verify(claims, never()).withdraw("RCL-9");
    }

    private static BillingDocumentResponse document(String number, BillingDocumentType type, BillingDocumentStatus status, String member) {
        BillingDocumentResponse document = mock(BillingDocumentResponse.class);
        when(document.documentNumber()).thenReturn(number);
        when(document.documentType()).thenReturn(type);
        when(document.status()).thenReturn(status);
        when(document.customerType()).thenReturn("MEMBER");
        when(document.customerCode()).thenReturn(member);
        return document;
    }
}
