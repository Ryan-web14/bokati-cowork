package com.sni.bokaticowork.features.inventory.admin.service.support;

import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.admin.dto.InventoryMovementReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class InventoryMovementReportPdfRenderer {

    private static final String TEMPLATE_NAME = "inventory/movement-report";

    static final ZoneId APP_ZONE = ZoneId.of("Africa/Brazzaville");
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final Locale appLocale;
    private final OperatorIdentityService operatorIdentity;
    private final BrandLogo brandLogo;
    private final SpringTemplateEngine templateEngine = buildTemplateEngine();

    public byte[] render(InventoryMovementReportResponse report) {
        String html = renderHtml(report);
        try {
            return PdfRenderer.render(html);
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate inventory movement PDF report", ex);
        }
    }

    private String renderHtml(InventoryMovementReportResponse report) {
        Context context = new Context(appLocale);
        context.setVariable("report", report);
        context.setVariable("generatedAt", GENERATED_AT.format(Instant.now().atZone(APP_ZONE)));
        context.setVariable("operator", operatorIdentity.current());
        context.setVariable("logo", brandLogo.base64());
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
