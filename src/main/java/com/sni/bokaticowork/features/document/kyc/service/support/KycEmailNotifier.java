package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.outbox.service.implementation.OutboxRecipientResolver;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
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
public class KycEmailNotifier {

    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final OutboxRecipientResolver recipientResolver;

    @Async
    public void notify(KycCase kycCase, String eventType) {
        if (kycCase == null || kycCase.getOwnerType() == null || kycCase.getOwnerId() == null) {
            return;
        }
        OutboxRecipientResolver.Recipient recipient =
                recipientResolver.resolveByOwnerId(kycCase.getOwnerType(), kycCase.getOwnerId());
        if (recipient == null || !StringUtils.hasText(recipient.email())) {
            log.warn("No KYC email recipient for case={} ownerType={} ownerId={}",
                    kycCase.getCode(), kycCase.getOwnerType(), kycCase.getOwnerId());
            return;
        }
        try {
            Context ctx = new Context(Locale.FRANCE);
            ctx.setVariable("recipientName",
                    StringUtils.hasText(recipient.displayName()) ? recipient.displayName() : "client");
            ctx.setVariable("kycCaseCode", kycCase.getCode());
            ctx.setVariable("eventType", eventType);
            ctx.setVariable("status",
                    kycCase.getStatus() != null ? kycCase.getStatus().name() : "N/A");

            String html = templateEngine.process("email/kyc-event", ctx);
            emailSender.sendHtmlEmail(recipient.email(), subject(eventType, kycCase.getCode()), html);
            log.info("KYC email sent to {} event={} case={}", recipient.email(), eventType, kycCase.getCode());
        } catch (MessagingException ex) {
            log.warn("Failed to send KYC email case={} event={}: {}", kycCase.getCode(), eventType, ex.getMessage());
        } catch (Exception ex) {
            log.warn("KYC email error case={} event={}: {}", kycCase.getCode(), eventType, ex.getMessage());
        }
    }

    private String subject(String eventType, String caseCode) {
        return switch (eventType) {
            case "KYC_CASE_APPROVED"  -> "Dossier KYC approuvé — " + caseCode;
            case "KYC_CASE_REJECTED"  -> "Dossier KYC : correction requise — " + caseCode;
            case "KYC_CASE_SUBMITTED" -> "Dossier KYC soumis — " + caseCode;
            case "KYC_RENEWAL_REQUIRED" -> "Renouvellement KYC requis — " + caseCode;
            default -> "Mise à jour dossier KYC — " + caseCode;
        };
    }
}
