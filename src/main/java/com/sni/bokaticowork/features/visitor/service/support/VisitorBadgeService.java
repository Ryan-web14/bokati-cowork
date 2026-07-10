package com.sni.bokaticowork.features.visitor.service.support;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.visitor.model.VisitorPass;
import com.sni.bokaticowork.features.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
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
        String html = templateEngine.process("visitor/visitor-badge", ctx);
        return renderPdf(html);
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
            doc.outputSettings()
                    .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                    .charset(java.nio.charset.StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(doc), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Unable to generate visitor badge PDF", e);
        }
    }

    private String fmt(java.time.Instant instant) {
        if (instant == null) return "";
        return DT_FMT.format(instant.atZone(ZoneId.systemDefault()));
    }
}