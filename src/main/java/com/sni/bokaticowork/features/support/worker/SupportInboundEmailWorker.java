package com.sni.bokaticowork.features.support.worker;

import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.Message;
import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentStorageService;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketAttachment;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.repository.TicketAttachmentRepository;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportInboundEmailWorker {

    private static final Pattern TICKET_NUMBER = Pattern.compile("(TCK-\\d+)");
    private static final int MAX_MESSAGES_PER_RUN = 25;

    private static final List<String> KNOWN_SIGNATURES = List.of(
            "Get Outlook for iOS", "Get Outlook for Android",
            "Sent from Outlook for iOS", "Sent from Outlook for Android",
            "Sent from my iPhone", "Sent from my iPad",
            "Sent from Samsung Mobile",
            "Envoyé depuis mon iPhone", "Envoyé depuis mon iPad", "Envoyé de mon iPhone"
    );

    private final DefaultEmailSender emailSender;
    private final SupportTicketService ticketService;
    private final SupportTicketRepository ticketRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final DocumentStorageService documentStorageService;

    @Value("${bokati.support.sender-email:supportela@elleaose.com}")
    private String supportInbox;

    @Scheduled(fixedDelayString = "${bokati.support.inbound-poll-delay-ms:60000}")
    public void pollInbox() {
        try {
            var response = emailSender.graphClient()
                    .users().byUserId(supportInbox)
                    .messages()
                    .get(config -> {
                        config.queryParameters.filter = "isRead eq false";
                        config.queryParameters.top = MAX_MESSAGES_PER_RUN;
                        config.queryParameters.select = new String[]{"id", "subject", "from", "body", "receivedDateTime", "hasAttachments"};
                    });

            if (response == null || response.getValue() == null || response.getValue().isEmpty()) return;

            List<Message> messages = response.getValue();
            log.info("Support inbound: {} unread message(s) in {}", messages.size(), supportInbox);
            for (Message msg : messages) processMessage(msg);

        } catch (Exception ex) {
            log.error("Support inbound poll failed for {}", supportInbox, ex);
        }
    }

    private void processMessage(Message msg) {
        String msgId = msg.getId();
        String subject = msg.getSubject() != null ? msg.getSubject() : "";

        markAsRead(msgId);

        Matcher m = TICKET_NUMBER.matcher(subject);
        if (!m.find()) {
            log.debug("Inbound email has no ticket number in subject · skipping: {}", subject);
            return;
        }
        String ticketNumber = m.group(1);

        Optional<SupportTicket> ticketOpt = ticketRepository.findByTicketNumber(ticketNumber);
        if (ticketOpt.isEmpty()) {
            log.warn("Inbound email references unknown ticket {}, skipping", ticketNumber);
            return;
        }

        String senderEmail = "";
        String senderName = "";
        if (msg.getFrom() != null && msg.getFrom().getEmailAddress() != null) {
            senderEmail = msg.getFrom().getEmailAddress().getAddress();
            senderName = msg.getFrom().getEmailAddress().getName();
        }
        if (!StringUtils.hasText(senderName)) senderName = senderEmail;

        String bodyText = extractReplyText(msg);
        if (!StringUtils.hasText(bodyText)) {
            log.debug("Inbound email for ticket {} has empty body after extraction, skipping", ticketNumber);
            return;
        }

        try {
            // #3 addEmailReply includes deduplication via graphMessageId
            var response = ticketService.addEmailReply(ticketNumber, msgId, senderEmail, senderName, bodyText);
            log.info("Added email reply from {} to ticket {}", senderEmail, ticketNumber);

            // #5 Store attachments if any
            if (Boolean.TRUE.equals(msg.getHasAttachments())) {
                SupportTicket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);
                if (ticket != null) {
                    processAttachments(msg.getId(), ticket, null);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process email reply to ticket {} from {}: {}", ticketNumber, senderEmail, ex.getMessage(), ex);
        }
    }

    // ── Attachments (#5) ─────────────────────────────────────────────

    private void processAttachments(String messageId, SupportTicket ticket, TicketMessage ticketMessage) {
        try {
            var attResponse = emailSender.graphClient()
                    .users().byUserId(supportInbox)
                    .messages().byMessageId(messageId)
                    .attachments()
                    .get();

            if (attResponse == null || attResponse.getValue() == null) return;

            int idx = 0;
            for (Attachment att : attResponse.getValue()) {
                if (!(att instanceof FileAttachment fa)) continue;
                if (Boolean.TRUE.equals(fa.getIsInline())) continue;

                byte[] content = fa.getContentBytes();
                String fileName = StringUtils.hasText(fa.getName()) ? fa.getName() : "attachment-" + idx;
                if (content == null || content.length == 0) continue;

                try {
                    String documentCode = "ATT-" + ticket.getTicketNumber() + "-" + Instant.now().toEpochMilli() + "-" + idx;
                    var stored = documentStorageService.storeBytes(
                            "support/" + ticket.getTicketNumber(), documentCode, 1, fileName, content);
                    String contentType = StringUtils.hasText(fa.getContentType()) ? fa.getContentType() : "application/octet-stream";

                    attachmentRepository.save(TicketAttachment.builder()
                            .ticket(ticket)
                            .message(ticketMessage)
                            .documentCode(documentCode)
                            .fileName(fileName)
                            .storageProvider(stored.provider())
                            .storagePath(stored.storagePath())
                            .contentType(contentType)
                            .fileSize((long) content.length)
                            .internal(false)
                            .active(true)
                            .build());

                    log.info("Stored attachment '{}' for ticket {}", fileName, ticket.getTicketNumber());
                    idx++;
                } catch (Exception ex) {
                    log.warn("Failed to store attachment '{}' for ticket {}: {}", fileName, ticket.getTicketNumber(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch attachments for message {}: {}", messageId, ex.getMessage());
        }
    }

    // ── Email body extraction ─────────────────────────────────────────

    private void markAsRead(String messageId) {
        try {
            Message update = new Message();
            update.setIsRead(true);
            emailSender.graphClient()
                    .users().byUserId(supportInbox)
                    .messages().byMessageId(messageId)
                    .patch(update);
        } catch (Exception ex) {
            log.warn("Could not mark inbound email {} as read: {}", messageId, ex.getMessage());
        }
    }

    private String extractReplyText(Message msg) {
        if (msg.getBody() == null || !StringUtils.hasText(msg.getBody().getContent())) return "";
        String raw = msg.getBody().getContent();
        boolean isHtml = msg.getBody().getContentType() != null
                && "html".equalsIgnoreCase(msg.getBody().getContentType().toString());
        String text = isHtml ? extractFromHtml(raw) : stripQuotedPlainText(raw);
        return removeKnownSignatures(text).trim();
    }

    private String extractFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        doc.select("#divRplyFwdMsg, #OFRHeader, #OFRBody").remove();

        Element hr = doc.selectFirst("hr");
        if (hr != null) {
            Node next;
            while ((next = hr.nextSibling()) != null) next.remove();
            hr.remove();
        }

        doc.select("blockquote, .gmail_quote, .yahoo_quoted").remove();
        return doc.text();
    }

    private String stripQuotedPlainText(String text) {
        if (!StringUtils.hasText(text)) return text;
        String[] separators = {"\nFrom:", "\r\nFrom:", "\nDe :", "\r\nDe :", "\n-----", "\r\n-----"};
        int cutAt = text.length();
        for (String sep : separators) {
            int idx = text.indexOf(sep);
            if (idx > 0 && idx < cutAt) cutAt = idx;
        }
        return text.substring(0, cutAt);
    }

    private String removeKnownSignatures(String text) {
        for (String sig : KNOWN_SIGNATURES) text = text.replace(sig, "");
        return text;
    }
}
