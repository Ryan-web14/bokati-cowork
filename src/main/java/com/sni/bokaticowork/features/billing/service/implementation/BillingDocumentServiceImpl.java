package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentClauseRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentDiscountRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateManualBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentClauseResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentDiscountResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.CustomerStatementResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.specification.criteria.BillingDocumentSearchCriteria;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.support.BillingCalculationService;
import com.sni.bokaticowork.features.billing.service.support.BillingCustomerSnapshotResolver;
import com.sni.bokaticowork.features.billing.service.support.BillingDocumentWriter;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import com.sni.bokaticowork.features.billing.service.support.BillingLifecycleSupport;
import com.sni.bokaticowork.features.billing.service.support.BillingNumberingSupport;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
public class BillingDocumentServiceImpl implements BillingDocumentService {

    private final BillingDocumentRepository documentRepository;
    private final BillableItemRepository billableItemRepository;
    private final BillingCalculationService calculationService;
    private final BillingCustomerSnapshotResolver customerSnapshotResolver;
    private final BillingNumberingSupport numberingSupport;
    private final BillingDocumentWriter documentWriter;
    private final BillingLifecycleSupport lifecycleSupport;
    private final BillingEventWriter eventWriter;
    private final BillingDocumentMapper mapper;

    @Override
    public BillingDocumentResponse create(CreateBillingDocumentRequest request) {
        BillingCustomerSnapshotResolver.CustomerSnapshot customer = customerSnapshotResolver.resolve(request);
        BillingCalculationService.CalculatedDocument calculation = calculationService.calculate(request.lines(), request.discounts());
        BillingDocument document = buildDocument(request, customer, calculation);
        BillingDocument saved = documentWriter.save(document, calculation, request.discounts(), request.clauses());
        eventWriter.write(saved, "BILLING_DOCUMENT_CREATED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse createManualInvoice(CreateManualBillingDocumentRequest request) {
        return create(toDocumentRequest(BillingDocumentType.INVOICE, request));
    }

    @Override
    public BillingDocumentResponse createManualQuote(CreateManualBillingDocumentRequest request) {
        return create(toDocumentRequest(BillingDocumentType.QUOTE, request));
    }

    @Override
    public BillingDocumentResponse createManualQuotation(CreateManualBillingDocumentRequest request) {
        return createManualQuote(request);
    }

    @Override
    public BillingDocumentResponse createInvoiceFromBillableItems(CreateInvoiceFromBillableItemsRequest request) {
        List<BillableItem> billableItems = resolveBillableItems(request.billableNumbers());
        BillableItem firstItem = billableItems.get(0);
        String currency = billableItems.get(0).getCurrency();
        List<CreateBillingDocumentLineRequest> lines = billableItems.stream()
                .map(item -> new CreateBillingDocumentLineRequest(
                        null,
                        lineTypeFromBillable(item),
                        item.getBillableNumber(),
                        item.getDescription(),
                        item.getMetadataJson(),
                        BigDecimal.ONE,
                        item.getAmount(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        Boolean.TRUE,
                        null,
                        null,
                        item.getSourceType(),
                        item.getSourceId()
                ))
                .toList();
        CreateBillingDocumentRequest createRequest = new CreateBillingDocumentRequest(
                BillingDocumentType.INVOICE,
                firstItem.getSubscriberType().name(),
                firstItem.getSubscriberCode(),
                null,
                null,
                null,
                null,
                "BILLABLE_ITEM",
                String.join(",", request.billableNumbers()),
                request.title(),
                request.description(),
                null,
                currency,
                request.issueDate(),
                request.dueDate(),
                null,
                lines,
                List.of(),
                defaultInvoiceClauses()
        );
        BillingDocumentResponse response = create(createRequest);
        BillingDocument invoice = serviceByNumber(response.documentNumber());
        billableItems.forEach(item -> {
            item.setInvoiceId(invoice.getId());
            item.setStatus(BillableItemStatus.INVOICED);
            billableItemRepository.save(item);
        });
        return mapper.toResponse(invoice);
    }

    @Override
    public BillingDocumentResponse issue(String documentNumber) {
        BillingDocument document = serviceByNumber(documentNumber);
        lifecycleSupport.ensureCanIssue(document);
        document.setStatus(document.getDocumentType() == BillingDocumentType.QUOTE ? BillingDocumentStatus.SENT : BillingDocumentStatus.ISSUED);
        document.setIssuedAt(Instant.now());
        BillingDocument saved = documentRepository.save(document);
        eventWriter.write(saved, "BILLING_DOCUMENT_ISSUED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse send(String documentNumber) {
        BillingDocument document = serviceByNumber(documentNumber);
        lifecycleSupport.ensureCanSend(document);
        document.setStatus(document.getDocumentType() == BillingDocumentType.QUOTE ? BillingDocumentStatus.SENT : BillingDocumentStatus.SENT);
        document.setSentAt(Instant.now());
        BillingDocument saved = documentRepository.save(document);
        eventWriter.write(saved, "BILLING_DOCUMENT_SENT", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse acceptQuote(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() != BillingDocumentStatus.DRAFT && quote.getStatus() != BillingDocumentStatus.SENT) {
            throw new BadRequestException("Only draft or sent quotes can be accepted");
        }
        quote.setStatus(BillingDocumentStatus.ACCEPTED);
        BillingDocument saved = documentRepository.save(quote);
        eventWriter.write(saved, "QUOTE_ACCEPTED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse rejectQuote(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        if (quote.getStatus() == BillingDocumentStatus.CONVERTED) {
            throw new BadRequestException("Converted quote cannot be rejected");
        }
        quote.setStatus(BillingDocumentStatus.REJECTED);
        quote.setBalanceDue(BigDecimal.ZERO);
        BillingDocument saved = documentRepository.save(quote);
        eventWriter.write(saved, "QUOTE_REJECTED", null);
        return mapper.toResponse(saved);
    }

    @Override
    public BillingDocumentResponse convertQuoteToInvoice(String quoteNumber) {
        BillingDocument quote = quote(quoteNumber);
        lifecycleSupport.ensureQuoteCanConvert(quote);
        BillingDocumentResponse quoteResponse = mapper.toResponse(quote);
        CreateBillingDocumentRequest request = new CreateBillingDocumentRequest(
                BillingDocumentType.INVOICE,
                quoteResponse.customerType(),
                quoteResponse.customerCode(),
                quoteResponse.customerName(),
                quoteResponse.customerEmail(),
                quoteResponse.customerPhone(),
                quoteResponse.billingAddressJson(),
                "QUOTE",
                quoteResponse.documentNumber(),
                quoteResponse.title(),
                quoteResponse.description(),
                quoteResponse.terms(),
                quoteResponse.currency(),
                LocalDate.now(),
                null,
                quoteResponse.metadataJson(),
                quoteResponse.lines().stream().map(this::toCreateLineRequest).toList(),
                quoteResponse.discounts().stream().map(this::toCreateDiscountRequest).toList(),
                quoteResponse.clauses().stream().map(this::toCreateClauseRequest).toList()
        );
        BillingDocumentResponse invoice = create(request);
        quote.setStatus(BillingDocumentStatus.CONVERTED);
        documentRepository.save(quote);
        eventWriter.write(quote, "QUOTE_CONVERTED", java.util.Map.of("invoiceNumber", invoice.documentNumber()));
        return invoice;
    }

    @Override
    public BillingDocumentResponse createCreditNote(String invoiceNumber, CreateCreditNoteRequest request) {
        BillingDocument invoice = serviceByNumber(invoiceNumber);
        lifecycleSupport.ensureCanPay(invoice);
        BigDecimal amount = request.amount() == null ? invoice.getBalanceDue() : request.amount();
        if (amount.signum() <= 0 || amount.compareTo(invoice.getTotalAmount()) > 0) {
            throw new BadRequestException("Invalid credit note amount");
        }
        List<CreateBillingDocumentLineRequest> lines = request.lines() == null || request.lines().isEmpty()
                ? List.of(new CreateBillingDocumentLineRequest(null, BillingLineType.ADJUSTMENT, invoice.getDocumentNumber(), request.reason(), null, BigDecimal.ONE, amount, BigDecimal.ZERO, BigDecimal.ZERO, false, BigDecimal.ZERO, BigDecimal.ZERO, "INVOICE", invoice.getDocumentNumber()))
                : request.lines();
        BillingDocumentResponse creditNote = create(new CreateBillingDocumentRequest(
                BillingDocumentType.CREDIT_NOTE,
                invoice.getCustomerType(),
                invoice.getCustomerCode(),
                invoice.getCustomerName(),
                invoice.getCustomerEmail(),
                invoice.getCustomerPhone(),
                invoice.getBillingAddressJson(),
                "INVOICE",
                invoice.getDocumentNumber(),
                "Avoir " + invoice.getDocumentNumber(),
                request.reason(),
                null,
                invoice.getCurrency(),
                LocalDate.now(),
                null,
                null,
                lines,
                List.of(),
                List.of(new CreateBillingDocumentClauseRequest("CREDIT_NOTE_REASON", "Motif de l'avoir", request.reason(), 1))
        ));
        if (Boolean.TRUE.equals(request.applyImmediately())) {
            applyCreditNote(creditNote.documentNumber());
        }
        return get(creditNote.documentNumber());
    }

    @Override
    public BillingDocumentResponse applyCreditNote(String creditNoteNumber) {
        BillingDocument creditNote = serviceByNumber(creditNoteNumber);
        if (creditNote.getDocumentType() != BillingDocumentType.CREDIT_NOTE || !"INVOICE".equalsIgnoreCase(creditNote.getSourceType())) {
            throw new BadRequestException("Credit note is not linked to an invoice");
        }
        BillingDocument invoice = applyPayment(creditNote.getSourceCode(), creditNote.getTotalAmount());
        creditNote.setStatus(BillingDocumentStatus.ISSUED);
        BillingDocument saved = documentRepository.save(creditNote);
        eventWriter.write(saved, "CREDIT_NOTE_APPLIED", java.util.Map.of("invoiceNumber", invoice.getDocumentNumber()));
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerStatementResponse customerStatement(String customerType, String customerCode, Pageable pageable) {
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        String normalizedCustomerCode = requiredCustomerCode(customerCode);
        String normalizedCustomerType = normalizeCustomerType(customerType, normalizedCustomerCode);
        var documents = documentRepository.statementDocuments(normalizedCustomerType, normalizedCustomerCode, unsortedPageable).map(mapper::toResponse);
        BigDecimal totalInvoiced = documents.getContent().stream().map(BillingDocumentResponse::totalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = documents.getContent().stream().map(BillingDocumentResponse::paidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = documents.getContent().stream().map(BillingDocumentResponse::balanceDue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CustomerStatementResponse(normalizedCustomerType, normalizedCustomerCode, totalInvoiced, totalPaid, balance, documents.getContent());
    }

    @Override
    public int markOverdueDocuments() {
        return documentRepository.markOverdueDocuments();
    }

    @Override
    public BillingDocumentResponse acceptQuotation(String quotationNumber) {
        return acceptQuote(quotationNumber);
    }

    @Override
    public BillingDocumentResponse rejectQuotation(String quotationNumber) {
        return rejectQuote(quotationNumber);
    }

    @Override
    public BillingDocumentResponse convertQuotationToInvoice(String quotationNumber) {
        return convertQuoteToInvoice(quotationNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public BillingDocumentResponse get(String documentNumber) {
        return mapper.toResponse(serviceByNumber(documentNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BillingDocumentResponse> list(BillingDocumentType type,
                                                           BillingDocumentStatus status,
                                                           String customerType,
                                                           String customerCode,
                                                           String sourceType,
                                                           String sourceCode,
                                                           LocalDate fromDate,
                                                           LocalDate toDate,
                                                           String searchText,
                                                           Pageable pageable) {
        BillingDocumentSearchCriteria criteria = new BillingDocumentSearchCriteria(
                type,
                status,
                trim(customerType),
                trim(customerCode),
                trim(sourceType),
                trim(sourceCode),
                fromDate,
                toDate,
                trim(searchText)
        );
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(documentRepository.search(
                criteria.typeValue(),
                criteria.statusValue(),
                criteria.customerType(),
                criteria.customerCode(),
                criteria.sourceType(),
                criteria.sourceCode(),
                criteria.fromDate(),
                criteria.toDate(),
                criteria.searchText(),
                unsortedPageable
        ).map(mapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public BillingDocument serviceByNumber(String documentNumber) {
        if (!StringUtils.hasText(documentNumber)) {
            throw new BadRequestException("Billing document number is required");
        }
        return documentRepository.findByDocumentNumber(documentNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Billing document not found"));
    }

    @Override
    public BillingDocument applyPayment(String documentNumber, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Payment amount must be positive");
        }
        BillingDocument document = serviceByNumber(documentNumber);
        lifecycleSupport.ensureCanPay(document);
        BigDecimal paid = document.getPaidAmount().add(amount).min(document.getTotalAmount());
        document.setPaidAmount(paid);
        document.setBalanceDue(document.getTotalAmount().subtract(paid));
        if (document.getBalanceDue().signum() == 0) {
            document.setStatus(BillingDocumentStatus.PAID);
            document.setPaidAt(Instant.now());
        } else {
            document.setStatus(BillingDocumentStatus.PARTIALLY_PAID);
        }
        return documentRepository.save(document);
    }

    @Override
    public BillingDocument reversePayment(String documentNumber, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Reversal amount must be positive");
        }
        BillingDocument document = serviceByNumber(documentNumber);
        BigDecimal paid = document.getPaidAmount().subtract(amount);
        if (paid.signum() < 0) {
            paid = BigDecimal.ZERO;
        }
        document.setPaidAmount(paid);
        document.setBalanceDue(document.getTotalAmount().subtract(paid));
        if (paid.signum() == 0) {
            document.setStatus(BillingDocumentStatus.ISSUED);
            document.setPaidAt(null);
        } else if (document.getBalanceDue().signum() > 0) {
            document.setStatus(BillingDocumentStatus.PARTIALLY_PAID);
        }
        return documentRepository.save(document);
    }

    @Override
    public BillingDocument cancelAndArchive(String documentNumber, String reason) {
        BillingDocument document = serviceByNumber(documentNumber);
        if (document.getStatus() != BillingDocumentStatus.CANCELLED) {
            document.setStatus(BillingDocumentStatus.CANCELLED);
            if (document.getCancelledAt() == null) {
                document.setCancelledAt(Instant.now());
            }
            document.setBalanceDue(BigDecimal.ZERO);
            if (document.getPaidAmount() == null || document.getPaidAmount().signum() == 0) {
                document.setPaidAt(null);
            }
            document = documentRepository.save(document);
            eventWriter.write(document, "BILLING_DOCUMENT_CANCELLED", billingActionDetails(reason, true));
        }
        if (document.getArchivedAt() == null) {
            document.setArchivedAt(Instant.now());
            document = documentRepository.save(document);
            eventWriter.write(document, "BILLING_DOCUMENT_ARCHIVED", billingActionDetails(reason, null));
        }
        return document;
    }

    private BillingDocument buildDocument(CreateBillingDocumentRequest request,
                                          BillingCustomerSnapshotResolver.CustomerSnapshot customer,
                                          BillingCalculationService.CalculatedDocument calculation) {
        return BillingDocument.builder()
                .documentNumber(numberingSupport.nextDocumentNumber(request.documentType(), customer.customerType()))
                .documentType(request.documentType())
                .status(BillingDocumentStatus.DRAFT)
                .customerType(customer.customerType())
                .customerCode(customer.customerCode())
                .customerName(customer.customerName())
                .customerEmail(customer.customerEmail())
                .customerPhone(customer.customerPhone())
                .billingAddressJson(customer.billingAddressJson())
                .sourceType(trim(request.sourceType()))
                .sourceCode(trim(request.sourceCode()))
                .title(trim(request.title()))
                .description(trim(request.description()))
                .terms(trim(request.terms()))
                .currency(request.currency().trim().toUpperCase())
                .subtotalAmount(calculation.subtotalAmount())
                .discountAmount(calculation.discountAmount())
                .taxableAmount(calculation.taxableAmount())
                .vatAmount(calculation.vatAmount())
                .additionalCentAmount(calculation.additionalCentAmount())
                .taxAmount(calculation.taxAmount())
                .totalAmount(calculation.totalAmount())
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(calculation.totalAmount())
                .issueDate(request.issueDate() == null ? LocalDate.now() : request.issueDate())
                .dueDate(request.dueDate())
                .metadataJson(trim(request.metadataJson()))
                .build();
    }

    private List<BillableItem> resolveBillableItems(List<String> billableNumbers) {
        List<BillableItem> items = new ArrayList<>();
        String currency = null;
        for (String number : billableNumbers) {
            BillableItem item = billableItemRepository.findByBillableNumber(number)
                    .orElseThrow(() -> new ResourceNotFoundException("Billable item not found: " + number));
            if (item.getStatus() != BillableItemStatus.PENDING && item.getStatus() != BillableItemStatus.SENT_TO_INVOICE) {
                throw new BadRequestException("Billable item is not invoiceable: " + number);
            }
            if (currency == null) {
                currency = item.getCurrency();
            } else if (!currency.equalsIgnoreCase(item.getCurrency())) {
                throw new BadRequestException("All billable items must use the same currency");
            }
            items.add(item);
        }
        return items;
    }

    private List<CreateBillingDocumentClauseRequest> defaultInvoiceClauses() {
        return List.of(new CreateBillingDocumentClauseRequest(
                "PAYMENT_TERMS",
                "Conditions de paiement",
                "La facture est payable selon les modalites convenues. Tout retard peut entrainer des restrictions de service.",
                1
        ));
    }

    private CreateBillingDocumentRequest toDocumentRequest(BillingDocumentType type, CreateManualBillingDocumentRequest request) {
        return new CreateBillingDocumentRequest(
                type,
                request.customerType(),
                request.customerCode(),
                request.customerName(),
                request.customerEmail(),
                request.customerPhone(),
                request.billingAddressJson(),
                request.sourceType(),
                request.sourceCode(),
                request.title(),
                request.description(),
                request.terms(),
                request.currency(),
                request.issueDate(),
                request.dueDate(),
                request.metadataJson(),
                request.lines(),
                request.discounts(),
                request.clauses()
        );
    }

    private BillingLineType lineTypeFromBillable(BillableItem item) {
        String sourceType = item.getSourceType() == null ? "" : item.getSourceType().toUpperCase();
        if (sourceType.contains("BOOKING")) {
            return BillingLineType.BOOKING;
        }
        if (sourceType.contains("PASS")) {
            return BillingLineType.PASS;
        }
        if (sourceType.contains("ADDON")) {
            return BillingLineType.ADDON;
        }
        if (sourceType.contains("SUBSCRIPTION")) {
            return BillingLineType.SUBSCRIPTION;
        }
        if (sourceType.contains("OVERAGE")) {
            return BillingLineType.OVERAGE;
        }
        if (sourceType.contains("INVENTORY") || sourceType.contains("ITEM") || sourceType.contains("PRODUCT")) {
            return BillingLineType.PRODUCT;
        }
        return BillingLineType.SERVICE;
    }

    private BillingDocument quote(String quoteNumber) {
        BillingDocument quote = serviceByNumber(quoteNumber);
        if (quote.getDocumentType() != BillingDocumentType.QUOTE) {
            throw new BadRequestException("Billing document is not a quote");
        }
        return quote;
    }

    private CreateBillingDocumentLineRequest toCreateLineRequest(BillingDocumentLineResponse line) {
        return new CreateBillingDocumentLineRequest(
                line.lineOrder(),
                line.lineType(),
                line.itemCode(),
                line.description(),
                line.detailedDescription(),
                line.quantity(),
                line.unitPrice(),
                line.discountRate(),
                line.discountAmount(),
                line.taxable(),
                line.vatRate(),
                line.additionalCentRate(),
                line.sourceType(),
                line.sourceCode()
        );
    }

    private CreateBillingDocumentDiscountRequest toCreateDiscountRequest(BillingDocumentDiscountResponse discount) {
        return new CreateBillingDocumentDiscountRequest(
                discount.discountCode(),
                discount.description(),
                discount.discountType(),
                discount.value()
        );
    }

    private CreateBillingDocumentClauseRequest toCreateClauseRequest(BillingDocumentClauseResponse clause) {
        return new CreateBillingDocumentClauseRequest(
                clause.clauseCode(),
                clause.title(),
                clause.body(),
                clause.displayOrder()
        );
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeCustomerType(String customerType, String customerCode) {
        String normalized = trim(customerType);
        if (normalized == null) {
            throw new BadRequestException("Customer type is required");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (normalized.equals("CLIENT") || normalized.equals("CUSTOMER")) {
            String inferred = inferCustomerTypeFromCode(customerCode);
            if (inferred != null) {
                return inferred;
            }
        }
        return switch (normalized) {
            case "MEMBRE" -> "MEMBER";
            case "CLIENT" -> "CUSTOMER";
            case "BUSINESS", "COMPANY", "ENTREPRISE" -> "BUSINESS_ENTITY";
            default -> normalized;
        };
    }

    private String requiredCustomerCode(String customerCode) {
        String normalized = trim(customerCode);
        if (normalized == null) {
            throw new BadRequestException("Customer code is required");
        }
        return normalized;
    }

    private String inferCustomerTypeFromCode(String customerCode) {
        if (!StringUtils.hasText(customerCode)) {
            return null;
        }
        String normalizedCode = customerCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedCode.startsWith("MBR-")) {
            return "MEMBER";
        }
        if (normalizedCode.startsWith("CUS-")) {
            return "CUSTOMER";
        }
        if (normalizedCode.startsWith("BUS-") || normalizedCode.startsWith("BIZ-")) {
            return "BUSINESS_ENTITY";
        }
        return null;
    }

    private java.util.Map<String, Object> billingActionDetails(String reason, Boolean archived) {
        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        if (StringUtils.hasText(reason)) {
            details.put("reason", reason.trim());
        }
        if (archived != null) {
            details.put("archived", archived);
        }
        return details;
    }
}
