package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.enums.EmailDeliveryStatus;
import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import com.sni.bokaticowork.core.communication.mailService.repository.EmailDeliveryLogRepository;
import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageRepository;
import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppMessageSender;
import com.sni.bokaticowork.features.billing.dto.response.BillingSendingResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSending;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentSendingRepository;
import com.sni.bokaticowork.features.billing.repository.BillingRecipientPreferenceRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.MemberBillingContact;
import com.sni.bokaticowork.security.admin.user.service.interfaces.StaffProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot C5 : le journal des envois d'une piece montre le statut reel du courriel, tire du journal
 * des courriels du socle : envoye avec sa date, ou en echec avec la raison.
 */
class BillingEmailStatusC5Test {

    @Test
    @SuppressWarnings("unchecked")
    void shouldShowTheRealStatusOfTheEmail() {
        BillingDocumentSendingRepository sendings = mock(BillingDocumentSendingRepository.class);
        EmailDeliveryLogRepository logs = mock(EmailDeliveryLogRepository.class);
        BillingSendingService service = new BillingSendingService(mock(BillingDocumentServiceImpl.class), mock(BillingEmailService.class),
                sendings, mock(BillingRecipientPreferenceRepository.class), mock(MemberBillingContact.class), mock(StaffProfileService.class),
                mock(ObjectProvider.class), mock(ObjectProvider.class));
        ReflectionTestUtils.setField(service, "emailLogs", logs);

        BillingDocumentSending sent = sending("EML-1");
        BillingDocumentSending refused = sending("EML-2");
        when(sendings.findByDocumentNumberOrderBySentAtDesc("FAC-9")).thenReturn(List.of(sent, refused));
        Instant at = Instant.parse("2026-11-13T09:00:05Z");
        when(logs.findByEmailNumber("EML-1")).thenReturn(Optional.of(log(EmailDeliveryStatus.SENT, at, null)));
        when(logs.findByEmailNumber("EML-2")).thenReturn(Optional.of(log(EmailDeliveryStatus.FAILED, null, "550 5.1.1 Adresse inconnue")));

        List<BillingSendingResponse> views = service.sendings("FAC-9");

        assertThat(views.get(0).status()).isEqualTo("SENT");
        assertThat(views.get(0).deliveredAt()).isEqualTo(at);
        assertThat(views.get(1).status()).isEqualTo("FAILED");
        assertThat(views.get(1).error()).contains("Adresse inconnue");
    }

    private static BillingDocumentSending sending(String emailNumber) {
        return BillingDocumentSending.builder().documentNumber("FAC-9").channel("EMAIL").recipientsTo("compta@org.cg")
                .status("QUEUED").providerReference(emailNumber).sentAt(Instant.parse("2026-11-13T09:00:00Z")).build();
    }

    private static EmailDeliveryLog log(EmailDeliveryStatus status, Instant sentAt, String error) {
        EmailDeliveryLog log = new EmailDeliveryLog();
        log.setStatus(status);
        log.setSentAt(sentAt);
        log.setLastError(error);
        return log;
    }
}
