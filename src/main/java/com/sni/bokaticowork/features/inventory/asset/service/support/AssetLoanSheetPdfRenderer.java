package com.sni.bokaticowork.features.inventory.asset.service.support;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetAssignment;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Locale;

@Component
public class AssetLoanSheetPdfRenderer {

    private static final String TEMPLATE_NAME = "inventory/asset-loan-sheet";

    private final SpringTemplateEngine templateEngine = buildTemplateEngine();

    public byte[] render(Asset asset, AssetAssignment assignment) {
        String html = renderHtml(asset, assignment);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate asset loan sheet PDF", ex);
        }
    }

    private String renderHtml(Asset asset, AssetAssignment assignment) {
        Context context = new Context(Locale.FRANCE);
        context.setVariable("asset", asset);
        context.setVariable("assignment", assignment);
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
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
