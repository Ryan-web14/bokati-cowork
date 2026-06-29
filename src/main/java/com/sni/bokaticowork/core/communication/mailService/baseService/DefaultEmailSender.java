package com.sni.bokaticowork.core.communication.mailService.baseService;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody;
import com.sni.bokaticowork.core.communication.mailService.config.MicrosoftGraphMailProperties;
import com.sni.bokaticowork.core.communication.mailService.dto.EmailRabbitMessage;
import com.sni.bokaticowork.core.communication.mailService.dto.response.EmailDeliveryResponse;
import com.sni.bokaticowork.core.communication.mailService.enums.EmailPriority;
import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import com.sni.bokaticowork.core.communication.mailService.service.EmailDeliveryTracker;
import com.sni.bokaticowork.core.communication.mailService.service.EmailRabbitPublisher;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.microsoft.graph.models.InternetMessageHeader;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class DefaultEmailSender {

    private static final String GRAPH_DEFAULT_SCOPE = "https://graph.microsoft.com/.default";
    private static final String BODY_TYPE_HTML = "HTML";
    private static final String BODY_TYPE_TEXT = "TEXT";

    private final JavaMailSender mailSender;
    private final MicrosoftGraphMailProperties graphProperties;
    private final EmailDeliveryTracker deliveryTracker;
    private final Executor taskExecutor;
    private final EmailRabbitPublisher rabbitPublisher;
    private volatile GraphServiceClient graphClient;

    @Value("${spring.mail.microsoft.graph.sender-email:no-reply@elleaose.com}")
    private String fromEmail;

    public DefaultEmailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                              MicrosoftGraphMailProperties graphProperties,
                              EmailDeliveryTracker deliveryTracker,
                              @Qualifier("taskExecutor") Executor taskExecutor,
                              EmailRabbitPublisher rabbitPublisher) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.graphProperties = graphProperties;
        this.deliveryTracker = deliveryTracker;
        this.taskExecutor = taskExecutor;
        this.rabbitPublisher = rabbitPublisher;
    }

    // ─────────────────── Async methods → publish to RabbitMQ ───────────────────

    public CompletableFuture<Boolean> sendEmail(String to, String subject, String content) {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, false, null, null);
        publishToRabbit(delivery.emailNumber(), to, subject, content, EmailPriority.NORMAL,
                null, null, null, null);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> sendHtmlEmail(String to, String subject, String content) throws MessagingException {
        return sendHtmlEmail(to, subject, content, EmailPriority.NORMAL);
    }

    public CompletableFuture<Boolean> sendHtmlEmail(String to, String subject, String content, EmailPriority priority) throws MessagingException {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, true, null, null);
        publishToRabbit(delivery.emailNumber(), to, subject, content, priority,
                null, null, null, null);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> sendHtmlEmailWithInlineImage(String to,
                                                                    String subject,
                                                                    String content,
                                                                    String contentId,
                                                                    byte[] imageBytes) {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, true, null, null);
        publishToRabbit(delivery.emailNumber(), to, subject, content, EmailPriority.NORMAL,
                null, null, contentId, imageBytes);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> sendHtmlEmailWithPdfAttachment(String to,
                                                                     String subject,
                                                                     String content,
                                                                     String attachmentName,
                                                                     byte[] attachmentBytes) {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, true, null, null);
        publishToRabbit(delivery.emailNumber(), to, subject, content, EmailPriority.NORMAL,
                attachmentName, attachmentBytes, null, null);
        return CompletableFuture.completedFuture(true);
    }

    public EmailDeliveryResponse queueEmail(String to, String subject, String content, boolean html) {
        return queueEmail(to, subject, content, html, null, null);
    }

    public EmailDeliveryResponse queueEmail(String to,
                                            String subject,
                                            String content,
                                            boolean html,
                                            String relatedType,
                                            String relatedCode) {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, html, relatedType, relatedCode);
        publishToRabbit(delivery.emailNumber(), to, subject, content, EmailPriority.NORMAL,
                null, null, null, null);
        return delivery;
    }

    public EmailDeliveryResponse queueEmail(String from,
                                            String to,
                                            String subject,
                                            String content,
                                            boolean html,
                                            String relatedType,
                                            String relatedCode) {
        EmailDeliveryResponse delivery = deliveryTracker.queue(
                providerName(), from, to, subject,
                html ? BODY_TYPE_HTML : BODY_TYPE_TEXT,
                content, relatedType, relatedCode);
        publishToRabbit(delivery.emailNumber(), to, subject, content, EmailPriority.NORMAL,
                null, null, null, null);
        return delivery;
    }

    public EmailDeliveryResponse retry(String emailNumber) {
        EmailDeliveryResponse delivery = deliveryTracker.resetForRetry(emailNumber);
        EmailDeliveryLog log = deliveryTracker.markSending(emailNumber);
        deliveryTracker.resetForRetry(emailNumber);
        publishToRabbit(emailNumber, log.getRecipientEmail(), log.getSubject(), log.getBodyContent(),
                EmailPriority.NORMAL, null, null, null, null);
        return delivery;
    }

    private void publishToRabbit(String emailNumber, String to, String subject, String content,
                                 EmailPriority priority,
                                 String attachmentName, byte[] attachmentBytes,
                                 String inlineImageContentId, byte[] inlineImageBytes) {
        EmailRabbitMessage message = new EmailRabbitMessage(
                emailNumber, to, null, subject, content, priority,
                null, null, null, null, null,
                attachmentName, attachmentBytes,
                inlineImageContentId, inlineImageBytes,
                0, Instant.now()
        );
        rabbitPublisher.publishEmail(message);
    }

    // ─────────────── Blocking methods — used by EmailConsumer only ─────────────

    private EmailDeliveryResponse createDelivery(String to,
                                                 String subject,
                                                 String content,
                                                 boolean html,
                                                 String relatedType,
                                                 String relatedCode) {
        return deliveryTracker.queue(
                providerName(),
                resolveOutboundMailboxEmail(),
                to,
                subject,
                html ? BODY_TYPE_HTML : BODY_TYPE_TEXT,
                content,
                relatedType,
                relatedCode
        );
    }

    public boolean sendHtmlEmailBlocking(String to, String subject, String content) {
        return sendWithGraph(resolveOutboundMailboxEmail(), to, subject, content, true);
    }

    public boolean sendHtmlEmailWithPdfAttachmentBlocking(String to,
                                                          String subject,
                                                          String content,
                                                          String attachmentName,
                                                          byte[] attachmentBytes) {
        return sendWithGraphPdfAttachment(resolveOutboundMailboxEmail(), to, subject, content, attachmentName, attachmentBytes);
    }

    public boolean sendWithGraphInlineImageBlocking(String to,
                                                     String subject,
                                                     String content,
                                                     String contentId,
                                                     byte[] imageBytes) {
        return sendWithGraphInlineImage(resolveOutboundMailboxEmail(), to, subject, content, contentId, imageBytes);
    }

    // ─────────────────── Microsoft Graph API implementation ───────────────────

    private boolean sendWithGraph(String sender, String to, String subject, String content, boolean html) {
        return sendWithGraph(sender, to, subject, content, html, Collections.emptyList());
    }

    private boolean sendWithGraph(String sender, String to, String subject, String content, boolean html,
                                   List<InternetMessageHeader> headers) {
        try {
            Message msg = message(to, subject, content, html);
            if (headers != null && !headers.isEmpty()) {
                msg.setInternetMessageHeaders(headers);
            }
            SendMailPostRequestBody requestBody = new SendMailPostRequestBody();
            requestBody.setMessage(msg);
            requestBody.setSaveToSentItems(true);

            graphClient().users().byUserId(sender).sendMail().post(requestBody);
            log.info("{} email sent via Microsoft Graph from {} to {}", html ? "HTML" : "Plain text", sender, to);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email via Microsoft Graph to {}", to, ex);
            throw new IllegalStateException("Microsoft Graph send failed: " + ex.getMessage(), ex);
        }
    }

    private boolean sendWithGraphInlineImage(String sender,
                                             String to,
                                             String subject,
                                             String content,
                                             String contentId,
                                             byte[] imageBytes) {
        try {
            Message msg = message(to, subject, content, true);

            FileAttachment inline = new FileAttachment();
            inline.setOdataType("#microsoft.graph.fileAttachment");
            inline.setName(contentId + ".png");
            inline.setContentType("image/png");
            inline.setContentId(contentId);
            inline.setIsInline(true);
            inline.setContentBytes(imageBytes);

            msg.setAttachments(List.of((Attachment) inline));

            SendMailPostRequestBody requestBody = new SendMailPostRequestBody();
            requestBody.setMessage(msg);
            requestBody.setSaveToSentItems(true);

            graphClient().users().byUserId(sender).sendMail().post(requestBody);
            log.info("HTML email with inline image sent via Microsoft Graph from {} to {}", sender, to);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email with inline image via Microsoft Graph to {}", to, ex);
            throw new IllegalStateException("Microsoft Graph inline image send failed: " + ex.getMessage(), ex);
        }
    }

    private boolean sendWithGraphPdfAttachment(String sender,
                                               String to,
                                               String subject,
                                               String content,
                                               String attachmentName,
                                               byte[] attachmentBytes) {
        try {
            if (attachmentBytes == null || attachmentBytes.length == 0) {
                throw new IllegalArgumentException("PDF attachment is empty");
            }

            SendMailPostRequestBody requestBody = new SendMailPostRequestBody();
            requestBody.setMessage(messageWithPdfAttachment(to, subject, content, attachmentName, attachmentBytes));
            requestBody.setSaveToSentItems(true);

            graphClient().users().byUserId(sender).sendMail().post(requestBody);
            log.info("HTML email with PDF attachment sent via Microsoft Graph from {} to {}", sender, to);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email with PDF attachment via Microsoft Graph to {}", to, ex);
            throw new IllegalStateException("Microsoft Graph attachment send failed: " + ex.getMessage(), ex);
        }
    }

    private Message message(String to, String subject, String content, boolean html) {
        Message message = new Message();
        message.setSubject(subject);

        ItemBody body = new ItemBody();
        body.setContentType(html ? BodyType.Html : BodyType.Text);
        body.setContent(content);
        message.setBody(body);

        message.setToRecipients(List.of(recipient(to)));
        return message;
    }

    private Message messageWithPdfAttachment(String to,
                                             String subject,
                                             String content,
                                             String attachmentName,
                                             byte[] attachmentBytes) {
        Message message = message(to, subject, content, true);

        FileAttachment attachment = new FileAttachment();
        attachment.setOdataType("#microsoft.graph.fileAttachment");
        attachment.setName(StringUtils.hasText(attachmentName) ? attachmentName.trim() : "document.pdf");
        attachment.setContentType("application/pdf");
        attachment.setContentBytes(attachmentBytes);

        message.setAttachments(List.of((Attachment) attachment));
        return message;
    }

    private Recipient recipient(String email) {
        EmailAddress address = new EmailAddress();
        address.setAddress(email);

        Recipient recipient = new Recipient();
        recipient.setEmailAddress(address);
        return recipient;
    }

    public GraphServiceClient graphClient() {
        GraphServiceClient client = graphClient;
        if (client == null) {
            synchronized (this) {
                client = graphClient;
                if (client == null) {
                    ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                            .tenantId(graphProperties.getTenantId())
                            .clientId(graphProperties.getClientId())
                            .clientSecret(graphProperties.getClientSecret())
                            .build();
                    client = new GraphServiceClient(credential, GRAPH_DEFAULT_SCOPE);
                    graphClient = client;
                }
            }
        }
        return client;
    }

    private String providerName() {
        return graphProperties.getSenderEmail();
    }

    private String resolveOutboundMailboxEmail() {
        if (StringUtils.hasText(graphProperties.getSenderEmail())) {
            return graphProperties.getSenderEmail().trim();
        }
        return StringUtils.hasText(fromEmail) ? fromEmail.trim() : graphProperties.getSenderEmail();
    }
}
