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
import com.sni.bokaticowork.core.communication.mailService.dto.response.EmailDeliveryResponse;
import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import com.sni.bokaticowork.core.communication.mailService.service.EmailDeliveryTracker;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
    private volatile GraphServiceClient graphClient;

    @Value("${spring.mail.microsoft.graph.sender-email:no-reply@elleaose.com}")
    private String fromEmail;

    public DefaultEmailSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                              MicrosoftGraphMailProperties graphProperties,
                              EmailDeliveryTracker deliveryTracker,
                              @Qualifier("taskExecutor") Executor taskExecutor) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.graphProperties = graphProperties;
        this.deliveryTracker = deliveryTracker;
        this.taskExecutor = taskExecutor;
    }

    public CompletableFuture<Boolean> sendEmail(String to, String subject, String content) {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, false, null, null);
        return CompletableFuture.supplyAsync(() -> processQueued(delivery.emailNumber()), taskExecutor);
    }

    public CompletableFuture<Boolean> sendHtmlEmail(String to, String subject, String content) throws MessagingException {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, true, null, null);
        return CompletableFuture.supplyAsync(() -> processQueued(delivery.emailNumber()), taskExecutor);
    }

    public CompletableFuture<Boolean> sendHtmlEmailWithInlineImage(String to,
                                                                    String subject,
                                                                    String content,
                                                                    String contentId,
                                                                    byte[] imageBytes) {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, true, null, null);
        return CompletableFuture.supplyAsync(
                () -> processQueuedWithInlineImage(delivery.emailNumber(), contentId, imageBytes),
                taskExecutor
        );
    }

    public CompletableFuture<Boolean> sendHtmlEmailWithPdfAttachment(String to,
                                                                     String subject,
                                                                     String content,
                                                                     String attachmentName,
                                                                     byte[] attachmentBytes) {
        EmailDeliveryResponse delivery = createDelivery(to, subject, content, true, null, null);
        return CompletableFuture.supplyAsync(
                () -> processQueuedWithPdfAttachment(delivery.emailNumber(), attachmentName, attachmentBytes),
                taskExecutor
        );
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
        CompletableFuture.runAsync(() -> processQueued(delivery.emailNumber()), taskExecutor);
        return delivery;
    }

    public EmailDeliveryResponse retry(String emailNumber) {
        EmailDeliveryResponse delivery = deliveryTracker.resetForRetry(emailNumber);
        CompletableFuture.runAsync(() -> processQueued(emailNumber), taskExecutor);
        return delivery;
    }

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

    private boolean processQueued(String emailNumber) {
        EmailDeliveryLog delivery = deliveryTracker.markSending(emailNumber);
        try {
            boolean sent = sendNow(delivery);
            if (sent) {
                deliveryTracker.markSent(emailNumber);
            } else {
                deliveryTracker.markFailed(emailNumber, "Email provider returned failure");
            }
            return sent;
        } catch (Exception ex) {
            deliveryTracker.markFailed(emailNumber, ex.getMessage());
            log.error("Failed to process email delivery {}", emailNumber, ex);
            return false;
        }
    }

    private boolean processQueuedWithInlineImage(String emailNumber, String contentId, byte[] imageBytes) {
        EmailDeliveryLog delivery = deliveryTracker.markSending(emailNumber);
        try {
            boolean sent = sendWithGraphInlineImage(
                    delivery.getRecipientEmail(),
                    delivery.getSubject(),
                    delivery.getBodyContent(),
                    contentId,
                    imageBytes
            );
            if (sent) {
                deliveryTracker.markSent(emailNumber);
            } else {
                deliveryTracker.markFailed(emailNumber, "Email provider returned failure");
            }
            return sent;
        } catch (Exception ex) {
            deliveryTracker.markFailed(emailNumber, ex.getMessage());
            log.error("Failed to process email delivery {} with inline image", emailNumber, ex);
            throw new CompletionException(ex);
        }
    }

    private boolean processQueuedWithPdfAttachment(String emailNumber, String attachmentName, byte[] attachmentBytes) {
        EmailDeliveryLog delivery = deliveryTracker.markSending(emailNumber);
        try {
            boolean sent = sendWithGraphPdfAttachment(
                    delivery.getRecipientEmail(),
                    delivery.getSubject(),
                    delivery.getBodyContent(),
                    attachmentName,
                    attachmentBytes
            );
            if (sent) {
                deliveryTracker.markSent(emailNumber);
            } else {
                deliveryTracker.markFailed(emailNumber, "Email provider returned failure");
            }
            return sent;
        } catch (Exception ex) {
            deliveryTracker.markFailed(emailNumber, ex.getMessage());
            log.error("Failed to process email delivery {} with PDF attachment", emailNumber, ex);
            throw new CompletionException(ex);
        }
    }

    private boolean sendNow(EmailDeliveryLog delivery) throws MessagingException {
        boolean html = BODY_TYPE_HTML.equalsIgnoreCase(delivery.getBodyType());
            return sendWithGraph(delivery.getRecipientEmail(), delivery.getSubject(), delivery.getBodyContent(), html);

//        if (html) {
//            return sendHtmlWithSmtp(delivery.getRecipientEmail(), delivery.getSubject(), delivery.getBodyContent());
//        }
        //return sendTextWithSmtp(delivery.getRecipientEmail(), delivery.getSubject(), delivery.getBodyContent());
    }

    /**
     * Envoi synchrone direct via Graph — utilisé par les processors outbox.
     * Bloque le thread appelant jusqu'à la réponse de l'API.
     * Lance une exception si l'envoi échoue → l'outbox peut retenter.
     */
    public boolean sendHtmlEmailBlocking(String to, String subject, String content) {
        return sendWithGraph(to, subject, content, true);
    }

    public boolean sendHtmlEmailWithPdfAttachmentBlocking(String to,
                                                          String subject,
                                                          String content,
                                                          String attachmentName,
                                                          byte[] attachmentBytes) {
        return sendWithGraphPdfAttachment(to, subject, content, attachmentName, attachmentBytes);
    }

    private boolean sendTextWithSmtp(String to, String subject, String content) {
        try {
            if (mailSender == null) {
                throw new IllegalStateException("No email provider configured. Microsoft Graph is incomplete and JavaMailSender is unavailable");
            }
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(resolveOutboundMailboxEmail());
            mail.setTo(to);
            mail.setSubject(subject);
            mail.setText(content);

            mailSender.send(mail);
            log.info("Email sent to {}", to);
            return true;
        } catch (MailException ex) {
            log.error("Error while sending email to {}", to, ex);
            throw new IllegalStateException("SMTP send failed: " + ex.getMessage(), ex);
        }
    }

    private boolean sendHtmlWithSmtp(String to, String subject, String content) throws MessagingException {
        try {
            if (mailSender == null) {
                throw new IllegalStateException("No email provider configured. Microsoft Graph is incomplete and JavaMailSender is unavailable");
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(resolveOutboundMailboxEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("HTML email sent to {}", to);
            return true;
        } catch (MessagingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to send HTML email to {}", to, ex);
            throw new IllegalStateException("SMTP HTML send failed: " + ex.getMessage(), ex);
        }
    }

    private boolean sendWithGraph(String to, String subject, String content, boolean html) {
        try {
            String sender = resolveOutboundMailboxEmail();
            SendMailPostRequestBody requestBody = new SendMailPostRequestBody();
            requestBody.setMessage(message(to, subject, content, html));
            requestBody.setSaveToSentItems(true);

            graphClient().users().byUserId(sender).sendMail().post(requestBody);
            log.info("{} email sent via Microsoft Graph from {} to {}", html ? "HTML" : "Plain text", sender, to);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email via Microsoft Graph to {}", to, ex);
            throw new IllegalStateException("Microsoft Graph send failed: " + ex.getMessage(), ex);
        }
    }

    private boolean sendWithGraphInlineImage(String to,
                                             String subject,
                                             String content,
                                             String contentId,
                                             byte[] imageBytes) {
        try {
            String sender = resolveOutboundMailboxEmail();
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

    private boolean sendWithGraphPdfAttachment(String to,
                                               String subject,
                                               String content,
                                               String attachmentName,
                                               byte[] attachmentBytes) {
        try {
            if (attachmentBytes == null || attachmentBytes.length == 0) {
                throw new IllegalArgumentException("PDF attachment is empty");
            }

            String sender = resolveOutboundMailboxEmail();
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

    private GraphServiceClient graphClient() {
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
