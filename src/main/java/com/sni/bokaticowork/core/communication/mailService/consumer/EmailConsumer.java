package com.sni.bokaticowork.core.communication.mailService.consumer;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.communication.mailService.dto.EmailRabbitMessage;
import com.sni.bokaticowork.core.communication.mailService.service.EmailDeliveryTracker;
import com.sni.bokaticowork.core.communication.mailService.service.GraphApiRateLimiter;
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
