package com.sni.bokaticowork.features.reporting.service.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.reporting.dto.response.FinancialDashboardResponse;
import com.sni.bokaticowork.features.reporting.dto.response.OccupancyReportResponse;
import com.sni.bokaticowork.features.reporting.service.interfaces.FinancialReportService;
import com.sni.bokaticowork.features.reporting.service.interfaces.OccupancyReportService;
import com.sni.bokaticowork.features.reporting.service.interfaces.ReportPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportPdfServiceImpl implements ReportPdfService {

    private final SpringTemplateEngine templateEngine;
    private final FinancialReportService financialReportService;
    private final OccupancyReportService occupancyReportService;

    @Override
    public byte[] financialDashboardPdf(LocalDate from, LocalDate to) {
        FinancialDashboardResponse data = financialReportService.dashboard(from, to);
        Context ctx = new Context(Locale.FRANCE);
        ctx.setVariable("report",      data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt",         new ReportFormatter());
        return renderPdf(templateEngine.process("reporting/financial-report", ctx));
    }

    @Override
    public byte[] occupancyPdf(LocalDate from, LocalDate to) {
        OccupancyReportResponse data = occupancyReportService.occupancy(from, to);
        Context ctx = new Context(Locale.FRANCE);
        ctx.setVariable("report",      data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt",         new ReportFormatter());
        return renderPdf(templateEngine.process("reporting/occupancy-report", ctx));
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
            throw new BadRequestException("Unable to generate report PDF", e);
        }
    }

    public static final class ReportFormatter {
        private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final String DASH = "—";

        public String money(BigDecimal v) {
            if (v == null) return DASH;
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
            nf.setMaximumFractionDigits(0);
            return nf.format(v.setScale(0, RoundingMode.HALF_UP)) + " XAF";
        }

        public String pct(BigDecimal v) {
            if (v == null) return DASH;
            return v.setScale(1, RoundingMode.HALF_UP).toPlainString() + " %";
        }

        public String date(LocalDate d) {
            return d == null ? DASH : DATE_FMT.format(d);
        }

        public String num(long v) {
            return NumberFormat.getIntegerInstance(Locale.FRANCE).format(v);
        }
    }
}