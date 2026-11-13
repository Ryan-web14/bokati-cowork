package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingDiscountGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot C3 : une remise hors limites part au N+1 ; l'emission attend l'accord, qui vaut pour l'offre
 * approuvee ; un refus laisse la piece en brouillon ; une offre modifiee relance une demande.
 */
class DiscountApprovalC3Test {

    private final BillingDiscountGuard guard = mock(BillingDiscountGuard.class);
    private final ApprovalEngine engine = mock(ApprovalEngine.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);
    private DiscountApprovalService service;

    private final BillingDiscountGuard.DiscountViolation tooMuch = new BillingDiscountGuard.DiscountViolation("ITEM_MAX_DISCOUNT", "BLOCK",
            "SRV-1", "Bureau", new BigDecimal("30"), new BigDecimal("10"), "« Bureau » : remise de 30 % au-delà de 10 %");
    private BillingDocument quote;
    private List<BillingDocumentLine> lines;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<ApprovalEngine> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(engine);
        service = new DiscountApprovalService(guard, provider, documents, new ObjectMapper());
        service.setPlatformSettings(settings);
        when(settings.text(eq(DiscountApprovalService.MODE_KEY), anyString())).thenReturn("APPROVAL");
        quote = BillingDocument.builder().documentNumber("DEV-1").documentType(BillingDocumentType.QUOTE).currency("XAF")
                .totalAmount(new BigDecimal("700000")).discountAmount(BigDecimal.ZERO).customerName("Société Mbote").build();
        lines = List.of(line("30"));
        when(documents.findByDocumentNumber("DEV-1")).thenReturn(Optional.of(quote));
        when(engine.submit(any())).thenAnswer(invocation -> request("APR-1", ApprovalStatus.PENDING));
    }

    @Test
    void shouldSendTheRequestToTheManagerAndWaitForTheAgreement() {
        assertThatThrownBy(() -> service.enforce(quote, lines, List.of(tooMuch), null))
                .isInstanceOf(BadRequestException.class)
                .satisfies(error -> assertThat(((BadRequestException) error).getReason()).isEqualTo("DEROGATION_MOTIF_REQUIS"));

        assertThatThrownBy(() -> service.enforce(quote, lines, List.of(tooMuch), "Client stratégique"))
                .isInstanceOf(DiscountApprovalService.PendingException.class)
                .hasMessageContaining("APR-1");
        assertThat(quote.getDiscountApprovalStatus()).isEqualTo("PENDING");
        assertThat(quote.getDiscountApprovalReason()).isEqualTo("Client stratégique");

        // Meme offre : on attend, aucune nouvelle demande.
        assertThatThrownBy(() -> service.enforce(quote, lines, List.of(tooMuch), "Client stratégique"))
                .isInstanceOf(DiscountApprovalService.PendingException.class);
        verify(engine, times(1)).submit(any());

        service.onDecision(request("APR-1", ApprovalStatus.APPROVED), outcome(true, "chef@elleaose.com", "D'accord"), DiscountApprovalService.APPROVED);
        assertThat(service.enforce(quote, lines, List.of(tooMuch), null)).isEqualTo("Client stratégique");
        assertThat(quote.getDiscountApprovedBy()).isEqualTo("chef@elleaose.com");
    }

    @Test
    void shouldAskAgainWhenTheOfferChangesAfterTheAgreement() {
        assertThatThrownBy(() -> service.enforce(quote, lines, List.of(tooMuch), "Client stratégique"))
                .isInstanceOf(DiscountApprovalService.PendingException.class);
        service.onDecision(request("APR-1", ApprovalStatus.APPROVED), outcome(true, "chef@elleaose.com", null), DiscountApprovalService.APPROVED);

        List<BillingDocumentLine> changed = List.of(line("35"));
        when(engine.submit(any())).thenAnswer(invocation -> request("APR-2", ApprovalStatus.PENDING));
        assertThatThrownBy(() -> service.enforce(quote, changed, List.of(tooMuch), "Client stratégique"))
                .isInstanceOf(DiscountApprovalService.PendingException.class)
                .hasMessageContaining("APR-2");
        verify(engine, times(2)).cancelSubject(eq("BILLING"), eq("DISCOUNT_OVERRIDE"), eq("DEV-1"), anyString());
    }

    @Test
    void shouldKeepTheDraftAfterARefusalUntilANewReasonIsGiven() {
        assertThatThrownBy(() -> service.enforce(quote, lines, List.of(tooMuch), "Client stratégique"))
                .isInstanceOf(DiscountApprovalService.PendingException.class);
        service.onDecision(request("APR-1", ApprovalStatus.REJECTED), outcome(false, "chef@elleaose.com", "Marge trop faible"),
                DiscountApprovalService.REJECTED);

        assertThatThrownBy(() -> service.enforce(quote, lines, List.of(tooMuch), "Client stratégique"))
                .satisfies(error -> {
                    assertThat(((BadRequestException) error).getReason()).isEqualTo("DEROGATION_REFUSEE");
                    assertThat(error.getMessage()).contains("Marge trop faible");
                });
        assertThatThrownBy(() -> service.enforce(quote, lines, List.of(tooMuch), "Contrat de 3 ans signé"))
                .isInstanceOf(DiscountApprovalService.PendingException.class);
        verify(engine, times(2)).submit(any());
    }

    @Test
    void shouldLetAnOverrideHolderOrThePermissionModePassDirectly() {
        when(guard.hasOverridePermission()).thenReturn(true);
        when(guard.enforce(any(), eq("Accord direction"))).thenReturn("Accord direction");
        assertThat(service.enforce(quote, lines, List.of(tooMuch), "Accord direction")).isEqualTo("Accord direction");

        when(guard.hasOverridePermission()).thenReturn(false);
        when(settings.text(eq(DiscountApprovalService.MODE_KEY), anyString())).thenReturn("PERMISSION");
        service.enforce(quote, lines, List.of(tooMuch), "Accord direction");
        verify(engine, never()).submit(any());
    }

    @Test
    void shouldIgnoreDocumentsWithinTheLimits() {
        BillingDiscountGuard.DiscountViolation warning = new BillingDiscountGuard.DiscountViolation("DOC", "WARN", null, null,
                BigDecimal.ONE, BigDecimal.ONE, "Alerte");
        assertThat(service.enforce(quote, lines, List.of(warning), null)).isNull();
        verify(engine, never()).submit(any());
    }

    private static BillingDocumentLine line(String discountRate) {
        return BillingDocumentLine.builder().itemCode("SRV-1").quantity(BigDecimal.ONE).unitPrice(new BigDecimal("1000000"))
                .discountRate(new BigDecimal(discountRate)).discountAmount(BigDecimal.ZERO).optional(false).build();
    }

    private static ApprovalRequest request(String number, ApprovalStatus status) {
        ApprovalRequest request = new ApprovalRequest();
        request.setNumber(number);
        request.setSubjectRef("DEV-1");
        request.setStatus(status);
        return request;
    }

    private static ApprovalDtos.Outcome outcome(boolean finalApproval, String actor, String comment) {
        return new ApprovalDtos.Outcome("APR-1", "DEV-1", finalApproval ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED, 1, 1,
                finalApproval, actor, null, null, null, comment);
    }
}
