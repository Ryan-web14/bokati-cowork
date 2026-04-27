package com.sni.bokaticowork.features.inventory.admin.service.support;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryMovementReportResponse;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Locale;

@Component
public class InventoryMovementReportPdfRenderer {

    private static final String TEMPLATE_NAME = "inventory/movement-report";

    private final SpringTemplateEngine templateEngine = buildTemplateEngine();

    public byte[] render(InventoryMovementReportResponse report) {
        String html = renderHtml(report);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate inventory movement PDF report", ex);
        }
    }

    private String renderHtml(InventoryMovementReportResponse report) {
        Context context = new Context(Locale.FRANCE);
        context.setVariable("report", report);
        context.setVariable("generatedAt", Instant.now().toString());
        return templateEngine.process(TEMPLATE_NAME, context);
    }

    private static SpringTemplateEngine buildTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
