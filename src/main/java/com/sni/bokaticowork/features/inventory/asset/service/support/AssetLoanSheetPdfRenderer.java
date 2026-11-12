package com.sni.bokaticowork.features.inventory.asset.service.support;

import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import lombok.RequiredArgsConstructor;
import com.sni.bokaticowork.features.inventory.asset.model.AssetAssignment;
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
public class AssetLoanSheetPdfRenderer {

    private static final String TEMPLATE_NAME = "inventory/asset-loan-sheet";

    /** Fuseau de l'exploitant · la date imprimee est celle de Pointe-Noire, pas l'heure UTC. */
    static final ZoneId APP_ZONE = ZoneId.of("Africa/Brazzaville");
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final Locale appLocale;
    private final OperatorIdentityService operatorIdentity;
    private final BrandLogo brandLogo;
    private final SpringTemplateEngine templateEngine = buildTemplateEngine();

    public byte[] render(Asset asset, AssetAssignment assignment) {
        String html = renderHtml(asset, assignment);
        try {
            return PdfRenderer.render(html);
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate asset loan sheet PDF", ex);
        }
    }

    private String renderHtml(Asset asset, AssetAssignment assignment) {
        Context context = new Context(appLocale);
        context.setVariable("asset", asset);
        context.setVariable("assignment", assignment);
        // Etait l'Instant ISO brut (2026-11-12T09:14:03.512Z) · illisible sur une piece signee.
        context.setVariable("generatedAt", formatGeneratedAt(Instant.now()));
        context.setVariable("operator", operatorIdentity.current());
        context.setVariable("logo", brandLogo.base64());
        return templateEngine.process(TEMPLATE_NAME, context);
    }

    static String formatGeneratedAt(Instant instant) {
        return GENERATED_AT.format(instant.atZone(APP_ZONE));
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
