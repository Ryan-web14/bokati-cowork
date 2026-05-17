package com.sni.bokaticowork.features.support.worker;

import com.microsoft.graph.models.Message;
import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.support.dto.SupportDtos.AddTicketMessageRequest;
import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupportInboundEmailWorker {

    private static final Pattern TICKET_NUMBER = Pattern.compile("(TCK-\\d+)");
    private static final int MAX_MESSAGES_PER_RUN = 25;

    // Common mobile email app footers that appear before the quoted original
    private static final List<String> KNOWN_SIGNATURES = List.of(
            "Get Outlook for iOS",
            "Get Outlook for Android",
            "Sent from Outlook for iOS",
            "Sent from Outlook for Android",
            "Sent from my iPhone",
            "Sent from my iPad",
            "Sent from Samsung Mobile",
            "Envoyé depuis mon iPhone",
            "Envoyé depuis mon iPad",
            "Envoyé de mon iPhone"
    );

    private final DefaultEmailSender emailSender;
    private final SupportTicketService ticketService;
    private final SupportTicketRepository ticketRepository;

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
                        config.queryParameters.select = new String[]{"id", "subject", "from", "body", "receivedDateTime"};
                    });

            if (response == null || response.getValue() == null || response.getValue().isEmpty()) return;

            List<Message> messages = response.getValue();
            log.info("Support inbound: {} unread message(s) in {}", messages.size(), supportInbox);

            for (Message msg : messages) {
                processMessage(msg);
            }
        } catch (Exception ex) {
            log.error("Support inbound poll failed for {}", supportInbox, ex);
        }
    }

    private void processMessage(Message msg) {
        String msgId = msg.getId();
        String subject = msg.getSubject() != null ? msg.getSubject() : "";

        // Mark as read immediately to avoid reprocessing on next cycle
        markAsRead(msgId);

        Matcher m = TICKET_NUMBER.matcher(subject);
        if (!m.find()) {
            log.debug("Inbound email has no ticket number in subject — skipping: {}", subject);
            return;
        }
        String ticketNumber = m.group(1);

        if (ticketRepository.findByTicketNumber(ticketNumber).isEmpty()) {
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
            ticketService.addMessage(ticketNumber, new AddTicketMessageRequest(
                    TicketSenderType.CLIENT,
                    senderEmail,
                    senderName,
                    bodyText,
                    false
            ));
            log.info("Added email reply from {} to ticket {}", senderEmail, ticketNumber);
        } catch (Exception ex) {
            log.error("Failed to add email reply to ticket {} from {}: {}", ticketNumber, senderEmail, ex.getMessage(), ex);
        }
    }

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

    /**
     * Extracts only the author's new text from an email reply, stripping the quoted
     * original email and common mobile signatures.
     *
     * Strategy (HTML emails):
     *  1. Remove Outlook reply containers (#divRplyFwdMsg, #OFRHeader).
     *  2. Remove everything after the first <hr> — Outlook's reply separator.
     *  3. Remove Gmail/Yahoo blockquotes.
     *  4. Convert remaining HTML to plain text via Jsoup.
     *  5. Strip known mobile-app signatures ("Get Outlook for iOS", etc.).
     *
     * Strategy (plain text emails):
     *  Cut at the first line that starts with "From:", "De :", or "-----".
     */
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

        // 1. Remove Outlook reply/forward containers
        doc.select("#divRplyFwdMsg, #OFRHeader, #OFRBody").remove();

        // 2. Remove everything after the first <hr> (Outlook mobile reply separator)
        Element hr = doc.selectFirst("hr");
        if (hr != null) {
            Node next;
            while ((next = hr.nextSibling()) != null) {
                next.remove();
            }
            hr.remove();
        }

        // 3. Remove Gmail / Yahoo quoted blocks
        doc.select("blockquote, .gmail_quote, .yahoo_quoted").remove();

        return doc.text();
    }

    private String stripQuotedPlainText(String text) {
        if (!StringUtils.hasText(text)) return text;
        // Each separator must appear at the start of a line
        String[] separators = {"\nFrom:", "\r\nFrom:", "\nDe :", "\r\nDe :", "\n-----", "\r\n-----"};
        int cutAt = text.length();
        for (String sep : separators) {
            int idx = text.indexOf(sep);
            if (idx > 0 && idx < cutAt) cutAt = idx;
        }
        return text.substring(0, cutAt);
    }

    private String removeKnownSignatures(String text) {
        for (String sig : KNOWN_SIGNATURES) {
            text = text.replace(sig, "");
        }
        return text;
    }
}
