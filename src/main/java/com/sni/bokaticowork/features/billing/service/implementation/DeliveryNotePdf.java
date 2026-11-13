package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.core.utils.BrandLogo;
import com.sni.bokaticowork.core.utils.pdf.PdfRenderer;
import com.sni.bokaticowork.features.billing.dto.DeliveryNoteDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Lot C4 : PDF du bon de livraison (sans prix par defaut), avec les cases de signature. */
@Component
@RequiredArgsConstructor
public class DeliveryNotePdf {

    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final SpringTemplateEngine templateEngine;
    private final Locale appLocale;
    private final BrandLogo brandLogo;
    private final ObjectProvider<OperatorIdentityService> operators;

    /** Ligne imprimee : quantites et prix sans zeros inutiles. */
    public record PrintLine(Integer lineOrder, String itemCode, String description, String unit, String quantity, String unitPrice) {
    }

    public byte[] render(DeliveryNoteDtos.NoteView note) {
        Context context = new Context(appLocale);
        OperatorIdentityService identity = operators.getIfAvailable();
        context.setVariable("note", note);
        context.setVariable("lines", note.lines().stream().map(line -> new PrintLine(line.lineOrder(), line.itemCode(),
                line.description(), line.unit(), line.quantity() == null ? null : line.quantity().stripTrailingZeros().toPlainString(),
                line.unitPrice() == null ? null : line.unitPrice().stripTrailingZeros().toPlainString())).toList());
        context.setVariable("sourceLabel", "QUOTE".equals(note.sourceDocumentType()) ? "devis" : "facture");
        context.setVariable("deliveredOn", note.deliveredAt() == null ? null
                : DateTimeFormatter.ofPattern("dd/MM/yyyy").format(note.deliveredAt().atZone(ZONE)));
        context.setVariable("address", plainAddress(note.deliveryAddress()));
        context.setVariable("logo", brandLogo.base64());
        context.setVariable("operator", identity != null ? identity.current() : OperatorIdentity.defaults());
        String html = templateEngine.process("billing/delivery-note", context);
        try {
            return PdfRenderer.render(html);
        } catch (Exception exception) {
            throw new BadRequestException("Impossible de produire le bon de livraison en PDF", exception);
        }
    }

    /** Adresse lisible : un JSON d'adresse est aplati en une ligne (valeurs seules). */
    static String plainAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (!text.startsWith("{")) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\"[^\"]*\"\\s*:\\s*\"([^\"]*)\"").matcher(text);
        while (matcher.find()) {
            if (!matcher.group(1).isBlank()) {
                if (!result.isEmpty()) {
                    result.append(", ");
                }
                result.append(matcher.group(1).trim());
            }
        }
        return result.isEmpty() ? null : result.toString();
    }
}
