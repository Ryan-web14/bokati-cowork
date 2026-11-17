package com.sni.bokaticowork.core.outbox.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.communication.mailService.interfaces.OutboxNotificationMailService;
import com.sni.bokaticowork.core.outbox.model.OutboxEvent;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot KG3 : tout evenement de dossier a un processeur ; seuls certains envoient un courriel ; un echec remonte. */
class KycOutboxEventProcessorKg3Test {

    private final OutboxNotificationMailService mail = mock(OutboxNotificationMailService.class);
    private final OutboxRecipientResolver recipients = mock(OutboxRecipientResolver.class);
    private final KycOutboxEventProcessor processor = new KycOutboxEventProcessor(new ObjectMapper(), mail, recipients);

    private OutboxEvent event(String type, String payload) {
        return OutboxEvent.builder().aggregateType("KYC_CASE").eventType(type).payload(payload).build();
    }

    @Test
    void everyCaseEventIsSupportedButOnlySomeAreMailed() {
        OutboxEvent note = event("KYC_CASE_NOTE_ADDED", "{\"ownerType\":\"CUSTOMER\",\"ownerId\":9}");
        assertThat(processor.supports(note)).isTrue();
        processor.process(note);
        verify(mail, never()).sendKycNotification(anyString(), anyString(), anyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    void aRequestedPieceIsMailedWithItsName() {
        when(recipients.resolveByOwnerId(DocumentOwnerType.CUSTOMER, 9L)).thenReturn(new OutboxRecipientResolver.Recipient("Awa", "awa@example.com"));
        processor.process(event("KYC_CASE_REQUIREMENT_ADDED",
                "{\"ownerType\":\"CUSTOMER\",\"ownerId\":9,\"kycCaseCode\":\"KYC-1\",\"documentTypeName\":\"Attestation fiscale\",\"required\":true}"));

        ArgumentCaptor<Map<String, Object>> vars = ArgumentCaptor.forClass(Map.class);
        verify(mail).sendKycNotification(eq("awa@example.com"), eq("Une pièce de plus pour votre dossier KYC - KYC-1"), vars.capture());
        assertThat(vars.getValue()).containsEntry("documentTypeName", "Attestation fiscale").containsEntry("required", true);
    }

    @Test
    void aQueueFailureIsRethrownSoTheOutboxRetries() {
        when(recipients.resolveByOwnerId(any(), any())).thenReturn(new OutboxRecipientResolver.Recipient("Awa", "awa@example.com"));
        when(mail.sendKycNotification(anyString(), anyString(), anyMap())).thenThrow(new RuntimeException("file indisponible"));

        assertThatThrownBy(() -> processor.process(event("KYC_RENEWAL_REQUIRED",
                "{\"ownerType\":\"CUSTOMER\",\"ownerId\":9,\"kycCaseCode\":\"KYC-1\"}")))
                .hasMessageContaining("file indisponible");
    }
}
