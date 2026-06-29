package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.fiscal.FiscalQrCodeService;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceStatus;
import com.sni.bokaticowork.features.billing.enums.BillingAdvanceType;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingSignatureStatus;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentPdfService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BillingDocumentPdfServiceImpl implements BillingDocumentPdfService {

    private final BillingDocumentService billingDocumentService;
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PawapayDepositRepository pawapayDepositRepository;
    private final FiscalQrCodeService fiscalQrCodeService;
    private final Locale appLocale;

    @Value("${app.verify-base-url:http://localhost:8080}")
    private String verifyBaseUrl;

    @Override
    public byte[] generatePdf(String documentNumber) {
        BillingDocumentResponse document = billingDocumentService.get(documentNumber);
        String html = renderHtml(document);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document jsoupDoc = org.jsoup.Jsoup.parse(html);
            jsoupDoc.outputSettings()
                    .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                    .charset(java.nio.charset.StandardCharsets.UTF_8)
                    .prettyPrint(false);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new org.jsoup.helper.W3CDom().fromJsoup(jsoupDoc), null);
            builder.toStream(out);
            builder.run();
            byte[] pdfBytes = out.toByteArray();
            String watermark = resolveWatermark(document.status());
            return watermark != null ? addWatermark(pdfBytes, watermark) : pdfBytes;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate billing document PDF", ex);
        }
    }

    private String resolveWatermark(BillingDocumentStatus status) {
        if (status == null) return null;
        return switch (status) {
            case DRAFT -> "BROUILLON";
            case OVERDUE -> "EN RETARD";
            case CANCELLED, VOIDED -> "ANNULÉ";
            default -> null;
        };
    }

    private byte[] addWatermark(byte[] pdfBytes, String text) throws Exception {
        try (PDDocument doc = PDDocument.load(pdfBytes);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = PDType1Font.HELVETICA_BOLD;
            float fontSize = 65f;
            float angle = (float) Math.toRadians(45);
            float textWidth = font.getStringWidth(text) / 1000f * fontSize;

            for (PDPage page : doc.getPages()) {
                PDRectangle box = page.getMediaBox();
                float cx = box.getWidth() / 2f;
                float cy = box.getHeight() / 2f;

                PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                gs.setNonStrokingAlphaConstant(0.15f);
                gs.setStrokingAlphaConstant(0.15f);

                try (PDPageContentStream cs = new PDPageContentStream(
                        doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.saveGraphicsState();
                    cs.setGraphicsStateParameters(gs);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                    cs.setTextMatrix(Matrix.getRotateInstance(angle,
                            cx - (textWidth / 2f) * (float) Math.cos(angle),
                            cy - (textWidth / 2f) * (float) Math.sin(angle)));
                    cs.showText(text);
                    cs.endText();
                    cs.restoreGraphicsState();
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String renderHtml(BillingDocumentResponse document) {
        if (BillingDocumentType.CREDIT_NOTE.equals(document.documentType())) {
            return renderCreditNoteHtml(document);
        }
        Context context = new Context(appLocale);
        context.setVariable("document", document);
        context.setVariable("generatedAt", LocalDate.now());
        context.setVariable("fmt", new BillingDocumentTemplateFormatter(document.currency(), objectMapper, appLocale));
        context.setVariable("qrCode", generateQrCode(document));
        context.setVariable("payments", fetchPaymentInfos(document.documentNumber()));
        context.setVariable("logo", loadLogoBase64());
        return templateEngine.process("billing/document", context);
    }

    private String renderCreditNoteHtml(BillingDocumentResponse document) {
        BillingDocumentTemplateFormatter fmt = new BillingDocumentTemplateFormatter(document.currency(), objectMapper, appLocale);
        Context context = new Context(appLocale);
        context.setVariable("creditNote", toCreditNoteView(document, fmt));
        context.setVariable("customer", toCustomerView(document));
        context.setVariable("fmt", fmt);
        context.setVariable("generatedAt", LocalDate.now());
        context.setVariable("logo", loadLogoBase64());
        return templateEngine.process("billing/avoir-note-credit", context);
    }

    private CreditNoteView toCreditNoteView(BillingDocumentResponse doc, BillingDocumentTemplateFormatter fmt) {
        List<CreditNoteLineView> lines = doc.lines() == null ? List.of() :
                doc.lines().stream()
                        .map(l -> new CreditNoteLineView(
                                l.description(),
                                l.detailedDescription(),
                                fmt.quantity(l.quantity()),
                                fmt.money(l.unitPrice()),
                                fmt.money(l.totalAmount())))
                        .toList();
        return new CreditNoteView(
                doc.documentNumber(),
                fmt.date(doc.issueDate()),
                doc.sourceCode() != null ? doc.sourceCode() : "—",
                doc.description() != null ? doc.description() : doc.title(),
                lines,
                fmt.money(doc.subtotalAmount()),
                fmt.money(doc.vatAmount()),
                fmt.money(doc.totalAmount())
        );
    }

    private CustomerView toCustomerView(BillingDocumentResponse doc) {
        return new CustomerView(doc.customerName(), doc.customerCode(), doc.customerEmail());
    }

    public record CreditNoteView(
            String number, String issueDate, String invoiceNumber, String reason,
            List<CreditNoteLineView> lines,
            String totalHT, String tax, String totalTTC) {}

    public record CreditNoteLineView(
            String label, String description, String quantity, String unitPrice, String total) {}

    public record CustomerView(String name, String id, String email) {}

    private String loadLogoBase64() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/static/images/logo.png")) {
            if (is == null) return null;
            return Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }

    private List<PaymentInfo> fetchPaymentInfos(String documentNumber) {
        return paymentAllocationRepository.findAllByBillingDocumentNumber(documentNumber)
                .stream()
                .map(allocation -> {
                    PaymentTransaction tx = allocation.getPaymentTransaction();
                    String depositId = null;
                    String payerPhone = null;
                    if (tx.getPaymentMethod() == PaymentMethod.MOBILE_MONEY) {
                        PawapayDeposit deposit = pawapayDepositRepository
                                .findByTransactionNumber(tx.getTransactionNumber())
                                .orElse(null);
                        if (deposit != null) {
                            depositId = deposit.getDepositId();
                            payerPhone = deposit.getPhoneNumber();
                        }
                    }
                    return new PaymentInfo(
                            tx.getPaymentMethod() != null ? tx.getPaymentMethod().name() : null,
                            tx.getProvider(),
                            tx.getProviderReference(),
                            depositId,
                            payerPhone,
                            allocation.getAllocatedAmount(),
                            tx.getCurrency(),
                            tx.getPaidAt()
                    );
                })
                .toList();
    }

    public record PaymentInfo(
            String method,
            String provider,
            String providerReference,
            String depositId,
            String payerPhone,
            BigDecimal allocatedAmount,
            String currency,
            Instant paidAt
    ) {}

    private String generateQrCode(BillingDocumentResponse document) {
        try {
            String content = buildQrContent(document);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 350, 350);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", stream);
            return Base64.getEncoder().encodeToString(stream.toByteArray());
        } catch (Exception ex) {
            return null;
        }
    }

    private static final DateTimeFormatter QR_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String buildQrContent(BillingDocumentResponse document) {
        return fiscalQrCodeService.buildContent(document);
    }

    public static final class BillingDocumentTemplateFormatter {

        private static final String EMPTY_VALUE = "—";
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        private final String currency;
        private final ObjectMapper objectMapper;
        private final Locale locale;

        public BillingDocumentTemplateFormatter(String currency, ObjectMapper objectMapper, Locale locale) {
            this.currency = currency;
            this.objectMapper = objectMapper;
            this.locale = locale;
        }

        public String money(BigDecimal amount) {
            if (amount == null) {
                return EMPTY_VALUE;
            }
            String formatted = amount(amount);
            return StringUtils.hasText(currency) ? formatted + " " + currency.trim() : formatted;
        }

        public String amount(BigDecimal amount) {
            if (amount == null) {
                return EMPTY_VALUE;
            }
            NumberFormat nf = NumberFormat.getNumberInstance(locale);
            nf.setMaximumFractionDigits(0);
            nf.setMinimumFractionDigits(0);
            return nf.format(amount.setScale(0, RoundingMode.HALF_UP));
        }

        public String quantity(BigDecimal quantity) {
            if (quantity == null) {
                return EMPTY_VALUE;
            }
            BigDecimal normalized = quantity.stripTrailingZeros();
            return normalized.scale() < 0 ? normalized.setScale(0).toPlainString() : normalized.toPlainString();
        }

        public String date(LocalDate value) {
            return value == null ? EMPTY_VALUE : DATE_FORMATTER.format(value);
        }

        public String timestamp(Instant value) {
            return value == null ? EMPTY_VALUE : DATE_TIME_FORMATTER.format(value.atZone(ZoneId.systemDefault()));
        }

        public String partyLabel(String customerType) {
            if (!StringUtils.hasText(customerType)) {
                return "Client";
            }
            return switch (customerType.trim().toUpperCase(Locale.ROOT)) {
                case "MEMBER" -> "Membre";
                case "BUSINESS", "BUSINESS_ENTITY" -> "Entreprise";
                default -> "Client";
            };
        }

        public String documentSubject(BillingDocumentResponse document) {
            if (document == null) {
                return EMPTY_VALUE;
            }
            return firstText(
                    document.title(),
                    document.description(),
                    sourceReference(document.sourceType(), document.sourceCode()),
                    document.documentNumber()
            );
        }

        public String sourceReference(String sourceType, String sourceCode) {
            String type = clean(sourceType);
            String code = clean(sourceCode);
            if (!StringUtils.hasText(type) && !StringUtils.hasText(code)) {
                return EMPTY_VALUE;
            }
            if (!StringUtils.hasText(type)) {
                return code;
            }
            if (!StringUtils.hasText(code)) {
                return humanize(type);
            }
            return humanize(type) + " " + code;
        }

        public List<String> addressLines(String billingAddressJson) {
            return jsonLines(billingAddressJson, List.of("createdAt", "updatedAt", "id"));
        }

        public String percent(BigDecimal rate) {
            if (rate == null) return EMPTY_VALUE;
            BigDecimal normalized = rate.stripTrailingZeros();
            String val = normalized.scale() <= 0
                    ? normalized.setScale(0).toPlainString()
                    : normalized.toPlainString();
            return val + " %";
        }

        public String advanceTypeLabel(BillingAdvanceType type) {
            if (type == null) return EMPTY_VALUE;
            return switch (type) {
                case PERCENTAGE -> "Pourcentage";
                case FIXED_AMOUNT -> "Montant fixe";
            };
        }

        public String advanceStatusLabel(BillingAdvanceStatus status) {
            if (status == null) return EMPTY_VALUE;
            return switch (status) {
                case PENDING -> "En attente";
                case PAID -> "Réglé";
                case CANCELLED -> "Annulé";
            };
        }

        public String signatureStatusLabel(BillingSignatureStatus status) {
            if (status == null) return EMPTY_VALUE;
            return switch (status) {
                case PENDING -> "En attente de signature";
                case SIGNED -> "Signé";
                case EXPIRED -> "Expiré";
            };
        }

        private List<String> jsonLines(String payload, List<String> excludedKeys) {
            if (!StringUtils.hasText(payload)) {
                return List.of();
            }
            try {
                JsonNode root = objectMapper.readTree(payload);
                List<String> lines = new ArrayList<>();
                collectJsonLines(root, lines, excludedKeys);
                return lines;
            } catch (Exception ex) {
                return List.of(payload.trim());
            }
        }

        private void collectJsonLines(JsonNode node, List<String> lines, List<String> excludedKeys) {
            if (node == null || node.isNull()) {
                return;
            }
            if (node.isValueNode()) {
                String value = clean(node.asText());
                if (StringUtils.hasText(value)) {
                    lines.add(value);
                }
                return;
            }
            if (node.isArray()) {
                for (JsonNode child : node) {
                    collectJsonLines(child, lines, excludedKeys);
                }
                return;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (excludedKeys.contains(field.getKey())) {
                    continue;
                }
                JsonNode value = field.getValue();
                if (value == null || value.isNull()) {
                    continue;
                }
                if (value.isValueNode()) {
                    String text = clean(value.asText());
                    if (StringUtils.hasText(text)) {
                        lines.add(humanize(field.getKey()) + ": " + text);
                    }
                    continue;
                }
                collectJsonLines(value, lines, excludedKeys);
            }
        }

        private String firstText(String... values) {
            for (String value : values) {
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
            return EMPTY_VALUE;
        }

        private String clean(String value) {
            return value == null ? null : value.trim();
        }

        private String humanize(String raw) {
            if (!StringUtils.hasText(raw)) {
                return "";
            }
            String normalized = raw.trim().replace('_', ' ');
            normalized = normalized.replaceAll("([a-z])([A-Z])", "$1 $2");
            String lower = normalized.toLowerCase(Locale.ROOT);
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }
}
