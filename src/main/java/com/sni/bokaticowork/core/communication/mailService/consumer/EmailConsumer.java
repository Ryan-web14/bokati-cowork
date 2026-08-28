package com.sni.bokaticowork.core.communication.mailService.consumer;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.dto.EmailRabbitMessage;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailDeliveryStatus;
import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import com.sni.bokaticowork.core.communication.mailService.service.EmailDeliveryTracker;
import com.sni.bokaticowork.core.communication.mailService.service.GraphApiRateLimiter;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final DefaultEmailSender emailSender;
    private final EmailDeliveryTracker deliveryTracker;
    private final GraphApiRateLimiter rateLimiter;

    @RabbitListener(queues = "email.critical", containerFactory = "criticalEmailListenerFactory")
    public void consumeCritical(EmailRabbitMessage message) {
        processEmail(message);
    }

    @RabbitListener(queues = "email.high", containerFactory = "highEmailListenerFactory")
    public void consumeHigh(EmailRabbitMessage message) {
        processEmail(message);
    }

    @RabbitListener(queues = "email.normal", containerFactory = "normalEmailListenerFactory")
    public void consumeNormal(EmailRabbitMessage message) {
        processEmail(message);
    }

    @RabbitListener(queues = "email.bulk", containerFactory = "bulkEmailListenerFactory")
    public void consumeBulk(EmailRabbitMessage message) {
        processEmail(message);
    }

    private void processEmail(EmailRabbitMessage message) {
        EmailDeliveryLog deliveryLog;
        try {
            deliveryLog = deliveryTracker.getEntity(message.emailNumber());
        } catch (ResourceNotFoundException ex) {
            // Nothing to track the send against. Nacking would loop the message through the DLQ
            // forever, so drop it here instead — an untracked send is worse than a lost one.
            log.error("No delivery log for email {} · dropping message to {}",
                    message.emailNumber(), message.to());
            return;
        }

        if (deliveryLog.getStatus() == EmailDeliveryStatus.SENT) {
            // A broker redelivery or a duplicate publish for a message that already went out.
            // Ack and stop, otherwise the recipient gets a second copy.
            log.warn("Email {} already delivered to {} on {} · skipping duplicate send",
                    message.emailNumber(), deliveryLog.getRecipientEmail(), deliveryLog.getSentAt());
            return;
        }

        rateLimiter.acquirePermission();

        deliveryTracker.markSending(message.emailNumber());
        try {
            boolean sent;
            if (message.attachmentBytes() != null && message.attachmentBytes().length > 0) {
                sent = emailSender.sendHtmlEmailWithPdfAttachmentBlocking(
                        message.from(), message.to(), message.subject(), message.htmlContent(),
                        message.attachmentName(), message.attachmentBytes());
            } else if (message.inlineImageBytes() != null && message.inlineImageBytes().length > 0) {
                sent = emailSender.sendWithGraphInlineImageBlocking(
                        message.from(), message.to(), message.subject(), message.htmlContent(),
                        message.inlineImageContentId(), message.inlineImageBytes());
            } else {
                sent = emailSender.sendHtmlEmailBlocking(
                        message.from(), message.to(), message.subject(), message.htmlContent());
            }

            if (sent) {
                deliveryTracker.markSent(message.emailNumber());
                log.info("Email {} sent to {} [priority={}]",
                        message.emailNumber(), message.to(), message.priority());
            } else {
                throw new IllegalStateException("Email sender returned false");
            }
        } catch (Exception ex) {
            deliveryTracker.markFailed(message.emailNumber(), ex.getMessage());
            throw new RuntimeException("Email delivery failed for " + message.emailNumber(), ex);
        }
    }
}
