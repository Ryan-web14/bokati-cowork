package com.sni.bokaticowork.features.reporting.service.implementation;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.reporting.dto.response.*;
import com.sni.bokaticowork.features.reporting.service.interfaces.*;
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
    private final ActivityReportService activityReportService;
    private final MemberProfileReportService memberProfileReportService;
    private final DebtRecoveryReportService debtRecoveryReportService;
    private final ResourceUtilizationReportService resourceUtilizationReportService;
    private final StockValuationReportService stockValuationReportService;
    private final SubscriptionReportService subscriptionReportService;
    private final Locale appLocale;

    @Override
    public byte[] financialDashboardPdf(LocalDate from, LocalDate to) {
        FinancialDashboardResponse data = financialReportService.dashboard(from, to);
        Context ctx = new Context(appLocale);
        ctx.setVariable("report",      data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt",         new ReportFormatter(appLocale));
        return renderPdf(templateEngine.process("reporting/financial-report", ctx));
    }

    @Override
    public byte[] occupancyPdf(LocalDate from, LocalDate to) {
        OccupancyReportResponse data = occupancyReportService.occupancy(from, to);
        Context ctx = new Context(appLocale);
        ctx.setVariable("report",      data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt",         new ReportFormatter(appLocale));
        return renderPdf(templateEngine.process("reporting/occupancy-report", ctx));
    }

    @Override
    public byte[] activityReportPdf(LocalDate from, LocalDate to, boolean compareWithPrevious) {
        ActivityReportResponse data = activityReportService.activityReport(from, to, compareWithPrevious);
        Context ctx = new Context(appLocale);
        ctx.setVariable("report", data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt", new ReportFormatter(appLocale));
        return renderPdf(templateEngine.process("reporting/activity-report", ctx));
    }

    @Override
    public byte[] memberProfileReportPdf(String memberId) {
        MemberProfileReportResponse data = memberProfileReportService.memberProfile(memberId);
        Context ctx = new Context(appLocale);
        ctx.setVariable("report", data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt", new ReportFormatter(appLocale));
        return renderPdf(templateEngine.process("reporting/member-profile-report", ctx));
    }

    @Override
    public byte[] debtRecoveryReportPdf(String sortBy) {
        DebtRecoveryReportResponse data = debtRecoveryReportService.debtRecovery(sortBy);
        Context ctx = new Context(appLocale);
        ctx.setVariable("report", data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt", new ReportFormatter(appLocale));
        return renderPdf(templateEngine.process("reporting/debt-recovery-report", ctx));
    }

    @Override
    public byte[] resourceUtilizationReportPdf(LocalDate from, LocalDate to) {
        ResourceUtilizationReportResponse data = resourceUtilizationReportService.resourceUtilization(from, to);
        Context ctx = new Context(appLocale);
        ctx.setVariable("report", data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt", new ReportFormatter(appLocale));
        return renderPdf(templateEngine.process("reporting/resource-utilization-report", ctx));
    }

    @Override
    public byte[] stockValuationReportPdf(int inactiveDays) {
        StockValuationReportResponse data = stockValuationReportService.stockValuation(inactiveDays);
        Context ctx = new Context(appLocale);
        ctx.setVariable("report", data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt", new ReportFormatter(appLocale));
        return renderPdf(templateEngine.process("reporting/stock-valuation-report", ctx));
    }

    @Override
    public byte[] subscriptionReportPdf(int months, int renewalDays) {
        SubscriptionReportResponse data = subscriptionReportService.subscriptionReport(months, renewalDays);
        Context ctx = new Context(appLocale);
        ctx.setVariable("report", data);
        ctx.setVariable("generatedAt", LocalDate.now());
        ctx.setVariable("fmt", new ReportFormatter(appLocale));
        return renderPdf(templateEngine.process("reporting/subscription-report", ctx));
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
        private static final String DASH = "";

        private final Locale locale;

        public ReportFormatter(Locale locale) {
            this.locale = locale;
        }

        public String money(BigDecimal v) {
            if (v == null) return DASH;
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
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
            return NumberFormat.getIntegerInstance(locale).format(v);
        }

        public String days(long d) {
            return d + " jour" + (d > 1 ? "s" : "");
        }

        public String variation(BigDecimal pct) {
            if (pct == null) return DASH;
            String sign = pct.signum() >= 0 ? "+" : "";
            return sign + pct.setScale(1, RoundingMode.HALF_UP).toPlainString() + " %";
        }
    }
}