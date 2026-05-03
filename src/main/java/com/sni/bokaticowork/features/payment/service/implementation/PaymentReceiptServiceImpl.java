package com.sni.bokaticowork.features.payment.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptAllocationResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentMethod;
import com.sni.bokaticowork.features.payment.model.PaymentAllocation;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.repository.PaymentAllocationRepository;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Entities;
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
import java.text.NumberFormat;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentReceiptServiceImpl implements PaymentReceiptService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PawapayDepositRepository pawapayDepositRepository;
    private final BillingDocumentService billingDocumentService;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SpringTemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    @Value("${app.verify-base-url:http://localhost:8080}")
    private String verifyBaseUrl;

    @Override
    public PaymentReceiptResponse getByTransactionNumber(String transactionNumber) {
        return toResponse(serviceTransactionByNumber(transactionNumber));
    }

    @Override
    public PaymentReceiptResponse getByReceiptNumber(String receiptNumber) {
        return toResponse(serviceTransactionByReceipt(receiptNumber));
    }

    @Override
    public byte[] generatePdfByTransactionNumber(String transactionNumber) {
        return renderPdf(toResponse(serviceTransactionByNumber(transactionNumber)));
    }

    @Override
    public byte[] generatePdfByReceiptNumber(String receiptNumber) {
        return renderPdf(toResponse(serviceTransactionByReceipt(receiptNumber)));
    }

    private PaymentTransaction serviceTransactionByNumber(String transactionNumber) {
        if (!StringUtils.hasText(transactionNumber)) {
            throw new BadRequestException("Payment transaction number is required");
        }
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionNumber(transactionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));
        return ensureReceiptIdentity(transaction);
    }

    private PaymentTransaction serviceTransactionByReceipt(String receiptNumber) {
        if (!StringUtils.hasText(receiptNumber)) {
            throw new BadRequestException("Receipt number is required");
        }
        PaymentTransaction transaction = paymentTransactionRepository.findByReceiptNumber(receiptNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Payment receipt not found"));
        return ensureReceiptIdentity(transaction);
    }

    private PaymentTransaction ensureReceiptIdentity(PaymentTransaction transaction) {
        boolean changed = false;
        if (!StringUtils.hasText(transaction.getReceiptNumber())) {
            transaction.setReceiptNumber(sequenceGenerator.next("receipt"));
            changed = true;
        }
        if (transaction.getReceiptIssuedAt() == null) {
            transaction.setReceiptIssuedAt(transaction.getPaidAt() == null ? Instant.now() : transaction.getPaidAt());
            changed = true;
        }
        return changed ? paymentTransactionRepository.save(transaction) : transaction;
    }

    private PaymentReceiptResponse toResponse(PaymentTransaction transaction) {
        List<PaymentAllocation> allocations = transaction.getId() == null
                ? List.of()
                : paymentAllocationRepository.findAllByPaymentTransactionId(transaction.getId());
        List<BillingDocumentResponse> allocationDocuments = allocations.stream()
                .map(PaymentAllocation::getBillingDocumentNumber)
                .distinct()
                .map(billingDocumentService::get)
                .toList();

        Map<String, BillingDocumentResponse> documentsByNumber = new LinkedHashMap<>();
        allocationDocuments.forEach(document -> documentsByNumber.put(document.documentNumber(), document));

        List<PaymentReceiptAllocationResponse> allocationResponses = allocations.stream()
                .map(allocation -> {
                    BillingDocumentResponse document = documentsByNumber.get(allocation.getBillingDocumentNumber());
                    return new PaymentReceiptAllocationResponse(
                            allocation.getBillingDocumentNumber(),
                            document == null ? null : document.documentType(),
                            document == null ? null : document.title(),
                            document == null ? null : document.totalAmount(),
                            allocation.getAllocatedAmount(),
                            document == null ? null : document.balanceDue(),
                            transaction.getCurrency()
                    );
                })
                .toList();

        BigDecimal allocatedAmount = allocationResponses.stream()
                .map(PaymentReceiptAllocationResponse::allocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal advanceAmount = transaction.getAmount().subtract(allocatedAmount).max(BigDecimal.ZERO);

        String depositId = null;
        String payerPhone = null;
        if (transaction.getPaymentMethod() == PaymentMethod.MOBILE_MONEY) {
            PawapayDeposit deposit = pawapayDepositRepository
                    .findByTransactionNumber(transaction.getTransactionNumber())
                    .orElse(null);
            if (deposit != null) {
                depositId = deposit.getDepositId();
                payerPhone = deposit.getPhoneNumber();
            }
        }

        PartySnapshot party = partySnapshot(transaction, allocationDocuments);
        return new PaymentReceiptResponse(
                transaction.getReceiptNumber(),
                transaction.getReceiptIssuedAt(),
                transaction.getTransactionNumber(),
                transaction.getPaymentIntent().getIntentNumber(),
                transaction.getPaymentMethod(),
                transaction.getStatus(),
                transaction.getProvider(),
                transaction.getProviderReference(),
                depositId,
                payerPhone,
                transaction.getPaymentIntent().getPurpose(),
                transaction.getPaymentIntent().getSourceType(),
                transaction.getPaymentIntent().getSourceCode(),
                transaction.getPaymentIntent().getCustomerType(),
                transaction.getPaymentIntent().getCustomerCode(),
                party.name(),
                party.email(),
                party.phone(),
                party.billingAddressJson(),
                transaction.getAmount(),
                allocatedAmount,
                advanceAmount,
                transaction.getCurrency(),
                transaction.getPaidAt(),
                transaction.getReceivedBy(),
                allocationResponses
        );
    }

    private PartySnapshot partySnapshot(PaymentTransaction transaction, List<BillingDocumentResponse> allocationDocuments) {
        if (!allocationDocuments.isEmpty()) {
            BillingDocumentResponse document = allocationDocuments.get(0);
            return new PartySnapshot(
                    document.customerName(),
                    document.customerEmail(),
                    document.customerPhone(),
                    document.billingAddressJson()
            );
        }

        String customerType = transaction.getPaymentIntent().getCustomerType();
        String customerCode = transaction.getPaymentIntent().getCustomerCode();
        if (!StringUtils.hasText(customerType) || !StringUtils.hasText(customerCode)) {
            return PartySnapshot.empty();
        }

        String normalizedType = customerType.trim().toUpperCase(Locale.ROOT);
        if ("MEMBER".equals(normalizedType)) {
            return memberRepository.findByMemberIdAndDeletedFalse(customerCode.trim())
                    .map(this::memberSnapshot)
                    .orElse(PartySnapshot.empty());
        }
        if ("CUSTOMER".equals(normalizedType)) {
            return customerRepository.findByCustomerId(customerCode.trim())
                    .map(this::customerSnapshot)
                    .orElse(PartySnapshot.empty());
        }
        if ("BUSINESS_ENTITY".equals(normalizedType) || "BUSINESS".equals(normalizedType) || "COMPANY".equals(normalizedType)) {
            return businessRepository.findByCode(customerCode.trim())
                    .map(this::businessSnapshot)
                    .orElse(PartySnapshot.empty());
        }
        return PartySnapshot.empty();
    }

    private PartySnapshot memberSnapshot(Member member) {
        return new PartySnapshot(clean(member.getDisplayName()), clean(member.getEmail()), clean(member.getPhone()), null);
    }

    private PartySnapshot customerSnapshot(Customer customer) {
        String name = customer.getType() == CustomerType.COMPANY
                ? clean(customer.getCompanyName())
                : clean(((customer.getFirstname() == null ? "" : customer.getFirstname()) + " " + (customer.getLastname() == null ? "" : customer.getLastname())).trim());
        return new PartySnapshot(name, clean(customer.getBillingEmail() != null ? customer.getBillingEmail() : customer.getEmail()), clean(customer.getPhone()), null);
    }

    private PartySnapshot businessSnapshot(BusinessEntity business) {
        return new PartySnapshot(clean(business.getName()), clean(business.getEmail()), clean(business.getPhone()), null);
    }

    private byte[] renderPdf(PaymentReceiptResponse receipt) {
        String html = renderHtml(receipt);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.jsoup.nodes.Document xhtml = toXhtmlDocument(html);
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withW3cDocument(new W3CDom().fromJsoup(xhtml), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate payment receipt PDF", ex);
        }
    }

    private String renderHtml(PaymentReceiptResponse receipt) {
        Context context = new Context(Locale.FRANCE);
        context.setVariable("receipt", receipt);
        context.setVariable("generatedAt", LocalDate.now());
        context.setVariable("fmt", new PaymentReceiptTemplateFormatter(receipt.currency(), objectMapper));
        context.setVariable("qrCode", generateQrCode(receipt));
        context.setVariable("logo", loadLogoBase64());
        return templateEngine.process("payment/receipt", context);
    }

    private String loadLogoBase64() {
        try (java.io.InputStream is = getClass().getResourceAsStream("/static/images/logo.png")) {
            if (is == null) return null;
            return Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }

    private static final DateTimeFormatter QR_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String generateQrCode(PaymentReceiptResponse receipt) {
        try {
            String content = buildQrContent(receipt);
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

    private String buildQrContent(PaymentReceiptResponse receipt) {
        return verifyBaseUrl.stripTrailing() + "/verify/receipt/" + receipt.receiptNumber();
    }

    private org.jsoup.nodes.Document toXhtmlDocument(String html) {
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        document.outputSettings()
                .syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset(StandardCharsets.UTF_8)
                .prettyPrint(false);
        return document;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record PartySnapshot(
            String name,
            String email,
            String phone,
            String billingAddressJson
    ) {
        private static PartySnapshot empty() {
            return new PartySnapshot(null, null, null, null);
        }
    }

    public static final class PaymentReceiptTemplateFormatter {

        private static final String EMPTY_VALUE = "—";
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        private final String currency;
        private final ObjectMapper objectMapper;

        public PaymentReceiptTemplateFormatter(String currency, ObjectMapper objectMapper) {
            this.currency = currency;
            this.objectMapper = objectMapper;
        }

        public String money(BigDecimal amount) {
            if (amount == null) {
                return EMPTY_VALUE;
            }
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
            nf.setMaximumFractionDigits(0);
            nf.setMinimumFractionDigits(0);
            String formatted = nf.format(amount.setScale(0, RoundingMode.HALF_UP));
            return StringUtils.hasText(currency) ? formatted + " " + currency.trim() : formatted;
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
                case "BUSINESS", "BUSINESS_ENTITY", "COMPANY" -> "Entreprise";
                default -> "Client";
            };
        }

        public List<String> addressLines(String billingAddressJson) {
            if (!StringUtils.hasText(billingAddressJson)) {
                return List.of();
            }
            try {
                JsonNode root = objectMapper.readTree(billingAddressJson);
                List<String> lines = new ArrayList<>();
                collectJsonLines(root, lines, List.of("createdAt", "updatedAt", "id"));
                return lines;
            } catch (Exception ex) {
                return List.of(billingAddressJson.trim());
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
                node.forEach(child -> collectJsonLines(child, lines, excludedKeys));
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
                } else {
                    collectJsonLines(value, lines, excludedKeys);
                }
            }
        }

        private String clean(String value) {
            return StringUtils.hasText(value) ? value.trim() : null;
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
