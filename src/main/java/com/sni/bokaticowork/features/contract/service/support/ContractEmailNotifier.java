package com.sni.bokaticowork.features.contract.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFileResult;
import com.sni.bokaticowork.features.document.documentMaster.service.support.DocumentFileReader;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractEmailNotifier {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final DocumentFileReader documentFileReader;
    private final Locale appLocale;

    @Async
    public void notifyGenerated(String recipientEmail,
                                String recipientName,
                                String templateCode,
                                String documentCode) {
        if (!StringUtils.hasText(recipientEmail)) {
            return;
        }
        try {
            Context ctx = new Context(appLocale);
            ctx.setVariable("recipientName", StringUtils.hasText(recipientName) ? recipientName : "client");
            ctx.setVariable("templateCode", templateCode != null ? templateCode : "");
            ctx.setVariable("documentCode", documentCode);
            ctx.setVariable("eventType", "CONTRACT_DRAFT_GENERATED");

            String html = templateEngine.process("email/contract-event", ctx);

            if (StringUtils.hasText(documentCode)) {
                try {
                    DocumentFileResult file = documentFileReader.read(documentCode);
                    String fileName = StringUtils.hasText(file.fileName()) ? file.fileName() : documentCode + ".pdf";
                    emailSender.sendHtmlEmailWithPdfAttachment(
                            recipientEmail,
                            "Votre contrat est prêt · Elle A Osé",
                            html,
                            fileName,
                            file.content()
                    );
                    log.info("Contract email with PDF sent to {} documentCode={}", recipientEmail, documentCode);
                    return;
                } catch (Exception ex) {
                    log.warn("Could not attach contract PDF {} · sending without attachment: {}", documentCode, ex.getMessage());
                }
            }

            emailSender.sendHtmlEmail(recipientEmail, "Votre contrat est prêt · Elle A Osé", html);
            log.info("Contract email sent to {} documentCode={}", recipientEmail, documentCode);

        } catch (MessagingException ex) {
            log.warn("Failed to send contract email to {}: {}", recipientEmail, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Contract email error for {}: {}", recipientEmail, ex.getMessage());
        }
    }
}
