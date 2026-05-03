package com.sni.bokaticowork.features.client.member.service.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberPdfGeneration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class MemberPdfGenerationImpl implements MemberPdfGeneration {


    @Override
    public byte[] generateMemberInformationPdf() {

        File memberPdf = new File("member/fiche-inscription");

        try(ByteArrayOutputStream out = new ByteArrayOutputStream()){
            Document doc = Jsoup.parse(memberPdf, "UTF-8");
            doc.outputSettings()
                    .syntax(Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                    .charset(java.nio.charset.StandardCharsets.UTF_8)
                    .prettyPrint(false);

                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(doc), null);
                builder.toStream(out);
                builder.run();
            return out.toByteArray();

        }catch(Exception e){
            throw new BadRequestException("Unable to generate member-information pdf", e.getCause());
        }


    }
}
