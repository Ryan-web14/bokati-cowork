package com.sni.bokaticowork.features.visitor.service.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.visitor.model.Visitor;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import com.sni.bokaticowork.features.visitor.repository.VisitorPassRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class VisitorEmailNotifier {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SpringTemplateEngine templateEngine;
    private final DefaultEmailSender emailSender;
    private final VisitorPassRepository passRepository;
    private final VisitorQrGenerator qrGenerator;

    private static final String QR_CONTENT_ID = "qr-visitor";

    @Async
    public void sendInvitation(VisitorPass pass) {
        Visitor visitor = pass.getVisitor();
        if (visitor == null || !StringUtils.hasText(visitor.getEmail())) {
            return;
        }
        try {
            byte[] qrBytes = qrGenerator.generateBytes(pass.getQrValue());
            Context ctx = new Context(Locale.FRANCE);
            ctx.setVariable("visitorName",  visitor.getFullName());
            ctx.setVariable("visitorEmail", visitor.getEmail());
            ctx.setVariable("hostName",     pass.getHostName());
            ctx.setVariable("passNumber",   pass.getPassNumber());
            ctx.setVariable("purpose",      pass.getPurpose());
            ctx.setVariable("validFrom",    fmt(pass.getValidFrom()));
            ctx.setVariable("validUntil",   fmt(pass.getValidUntil()));
            String html = templateEngine.process("visitor/visitor-invitation", ctx);
            if (qrBytes != null) {
                emailSender.sendHtmlEmailWithInlineImage(visitor.getEmail(),
                        "Votre invitation visiteur — Elle A Osé Bokati Cowork", html,
                        QR_CONTENT_ID, qrBytes);
            } else {
                emailSender.sendHtmlEmail(visitor.getEmail(),
                        "Votre invitation visiteur — Elle A Osé Bokati Cowork", html);
            }
            pass.setInvitationSentAt(java.time.Instant.now());
            passRepository.save(pass);
        } catch (MessagingException e) {
            log.warn("Failed to send visitor invitation email to {}: {}", visitor.getEmail(), e.getMessage());
        }
    }

    private String fmt(java.time.Instant instant) {
        if (instant == null) return "—";
        return DT_FMT.format(instant.atZone(ZoneId.systemDefault()));
    }
}