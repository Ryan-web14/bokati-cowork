package com.sni.bokaticowork.core.communication.mailService.baseService;

import com.sni.bokaticowork.core.communication.mailService.config.MicrosoftGraphMailProperties;
import com.sni.bokaticowork.core.communication.mailService.dto.response.EmailDeliveryResponse;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailDeliveryStatus;
import com.sni.bokaticowork.core.communication.mailService.service.EmailDedupKeyFactory;
import com.sni.bokaticowork.core.communication.mailService.service.EmailDeliveryTracker;
import com.sni.bokaticowork.core.communication.mailService.service.EmailRabbitPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultEmailSenderDedupTest {

    private static final EmailDedupKeyFactory.Keys KEYS =
            new EmailDedupKeyFactory.Keys("key-current", List.of("key-current", "key-previous"));

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
    private final MicrosoftGraphMailProperties graphProperties = mock(MicrosoftGraphMailProperties.class);
    private final EmailDeliveryTracker deliveryTracker = mock(EmailDeliveryTracker.class);
    private final Executor taskExecutor = Runnable::run;
    private final EmailRabbitPublisher rabbitPublisher = mock(EmailRabbitPublisher.class);
    private final EmailDedupKeyFactory dedupKeyFactory = mock(EmailDedupKeyFactory.class);

    private final DefaultEmailSender sender = new DefaultEmailSender(
            mailSenderProvider, graphProperties, deliveryTracker, taskExecutor, rabbitPublisher, dedupKeyFactory);

    @Test
    void shouldQueueAndPublishWhenNoDuplicateExists() {
        when(graphProperties.getSenderEmail()).thenReturn("no-reply@elleaose.com");
        when(dedupKeyFactory.build(any(), any(), any(), any())).thenReturn(KEYS);
        when(deliveryTracker.findByDedupKeys(KEYS.lookupKeys())).thenReturn(Optional.empty());
        when(deliveryTracker.queue(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(delivery("EML-001"));

        EmailDeliveryResponse response = sender.queueEmail(
                "jane@example.com", "Facture INV-001", "<p>body</p>", true, "BILLING", "INV-001");

        assertThat(response.emailNumber()).isEqualTo("EML-001");
        verify(rabbitPublisher, times(1)).publishEmail(any());
    }

    @Test
    void shouldNotPublishAgainWhenTheSameEmailIsAlreadyQueued() {
        when(dedupKeyFactory.build(any(), any(), any(), any())).thenReturn(KEYS);
        when(deliveryTracker.findByDedupKeys(KEYS.lookupKeys())).thenReturn(Optional.of(delivery("EML-001")));

        EmailDeliveryResponse response = sender.queueEmail(
                "jane@example.com", "Facture INV-001", "<p>body</p>", true, "BILLING", "INV-001");

        // The caller still gets the original delivery, so nothing downstream sees a failure.
        assertThat(response.emailNumber()).isEqualTo("EML-001");
        verify(deliveryTracker, never()).queue(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), any(), anyString());
        verify(rabbitPublisher, never()).publishEmail(any());
    }

    @Test
    void shouldNotPublishWhenAConcurrentInstanceWinsTheUniqueIndexRace() {
        when(graphProperties.getSenderEmail()).thenReturn("no-reply@elleaose.com");
        when(dedupKeyFactory.build(any(), any(), any(), any())).thenReturn(KEYS);
        when(deliveryTracker.findByDedupKeys(KEYS.lookupKeys()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(delivery("EML-WINNER")));
        when(deliveryTracker.queue(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), any(), any(), anyString()))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates ux_email_delivery_dedup_key"));

        EmailDeliveryResponse response = sender.queueEmail(
                "jane@example.com", "Facture INV-001", "<p>body</p>", true, "BILLING", "INV-001");

        assertThat(response.emailNumber()).isEqualTo("EML-WINNER");
        verify(rabbitPublisher, never()).publishEmail(any());
    }

    @Test
    void shouldRefuseToRetryAnAlreadyDeliveredEmail() {
        when(deliveryTracker.getEntity("EML-001")).thenReturn(
                com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog.builder()
                        .emailNumber("EML-001")
                        .status(EmailDeliveryStatus.SENT)
                        .sentAt(Instant.now())
                        .build());

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> sender.retry("EML-001")))
                .isInstanceOf(com.sni.bokaticowork.core.exception.customs.BadRequestException.class);
        verify(rabbitPublisher, never()).publishEmail(any());
    }

    private EmailDeliveryResponse delivery(String emailNumber) {
        Instant now = Instant.now();
        return new EmailDeliveryResponse(
                emailNumber, "no-reply@elleaose.com", "no-reply@elleaose.com", "jane@example.com",
                "Facture INV-001", "HTML", EmailDeliveryStatus.QUEUED, 0, null,
                "BILLING", "INV-001", now, now, null, null);
    }
}
