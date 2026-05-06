package com.sni.bokaticowork.features.client.member.service.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberPdfGeneration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MemberPdfGenerationImpl implements MemberPdfGeneration {

    private final SpringTemplateEngine templateEngine;

    @Override
    public byte[] generateMemberInformationPdf() {
        return renderPdf("member/fiche-inscription", new Context(Locale.FRANCE));
    }

    @Override
    public byte[] generateFicheClientPdf() {
        return renderPdf("member/fiche-client", new Context(Locale.FRANCE));
    }

    private byte[] renderPdf(String templateName, Context context) {
        String html = templateEngine.process(templateName, context);
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
            throw new BadRequestException("Unable to generate PDF: " + templateName, e);
        }
    }
}