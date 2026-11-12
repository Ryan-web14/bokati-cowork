package com.sni.bokaticowork.features.visitor.service.support;

import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import com.sni.bokaticowork.features.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VisitorBadgeService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SpringTemplateEngine templateEngine;
    private final VisitorPassRepository passRepository;
    private final VisitorQrGenerator qrGenerator;
    private final Locale appLocale;
    private final OperatorIdentityService operatorIdentity;

    public byte[] generateBadge(String passNumber) {
        VisitorPass pass = passRepository.findByPassNumber(passNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Visitor pass not found: " + passNumber));
        String qrCode = qrGenerator.generateBase64(pass.getQrValue());
        Context ctx = new Context(appLocale);
        ctx.setVariable("pass",        pass);
        ctx.setVariable("visitor",     pass.getVisitor());
        ctx.setVariable("qrCode",      qrCode);
        ctx.setVariable("validFrom",   fmt(pass.getValidFrom()));
        ctx.setVariable("validUntil",  fmt(pass.getValidUntil()));
        ctx.setVariable("generatedAt", java.time.LocalDate.now());
        // Badge sur fond sombre · le logo encre y serait invisible, seule l'identite est reprise.
        ctx.setVariable("operator", operatorIdentity.current());
        String html = templateEngine.process("visitor/visitor-badge", ctx);
        return renderPdf(html);
    }

    private byte[] renderPdf(String html) {
        try {
            return PdfRenderer.render(html);
        } catch (Exception e) {
            throw new BadRequestException("Unable to generate visitor badge PDF", e);
        }
    }

    private String fmt(java.time.Instant instant) {
        if (instant == null) return "";
        return DT_FMT.format(instant.atZone(ZoneId.systemDefault()));
    }
}