package com.sni.bokaticowork.features.billing.mapper.decorator;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentAdvanceResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentClauseResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentDiscountResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentSignatureResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentTaxResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingRecoverableResponse;
import com.sni.bokaticowork.features.billing.dto.response.EarlyPaymentDiscountResponse;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentAdvance;
import com.sni.bokaticowork.features.billing.model.BillingDocumentClause;
import com.sni.bokaticowork.features.billing.model.BillingDocumentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentEarlyPaymentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSignature;
import com.sni.bokaticowork.features.billing.model.BillingDocumentTax;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentAdvanceRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentClauseRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEarlyPaymentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRecoverableRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentSignatureRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentTaxRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public abstract class BillingDocumentMapperDecorator implements BillingDocumentMapper {

    @Autowired
    @Qualifier("delegate")
    private BillingDocumentMapper delegate;

    @Autowired
    private BillingDocumentLineRepository lineRepository;

    @Autowired
    private BillingDocumentDiscountRepository discountRepository;

    @Autowired
    private BillingDocumentTaxRepository taxRepository;

    @Autowired
    private BillingDocumentClauseRepository clauseRepository;

    @Autowired
    private BillingDocumentAdvanceRepository advanceRepository;

    @Autowired
    private BillingDocumentEarlyPaymentDiscountRepository earlyPaymentDiscountRepository;

    @Autowired
    private BillingDocumentSignatureRepository signatureRepository;

    @Autowired
    private BillingDocumentRecoverableRepository recoverableRepository;

    @Autowired
    private TransactionContextResolver contextResolver;

    @Override
    public BillingDocumentResponse toResponse(BillingDocument document) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(document.getCustomerType(), document.getCustomerCode());
        TransactionContextResolver.SourceView source = contextResolver.resolveBillingDocumentSource(document);
        List<BillingDocumentLine> allLines = lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document);
        BigDecimal optionsTotal = allLines.stream()
                .filter(l -> Boolean.TRUE.equals(l.getOptional()))
                .map(BillingDocumentLine::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BillingDocumentResponse(
                document.getDocumentNumber(),
                document.getDocumentType(),
                document.getStatus(),
                document.getCustomerType(),
                document.getCustomerCode(),
                document.getCustomerName(),
                document.getCustomerEmail(),
                document.getCustomerPhone(),
                document.getBillingAddressJson(),
                party.registered(),
                document.getSourceType(),
                document.getSourceCode(),
                source.type(),
                source.code(),
                source.label(),
                source.registered(),
                document.getTitle(),
                document.getDescription(),
                document.getTerms(),
                document.getCurrency(),
                document.getSubtotalAmount(),
                document.getDiscountAmount(),
                document.getTaxableAmount(),
                document.getVatAmount(),
                document.getAdditionalCentAmount(),
                document.getTaxAmount(),
                document.getTotalAmount(),
                document.getPaidAmount(),
                document.getBalanceDue(),
                optionsTotal,
                document.getIssueDate(),
                document.getDueDate(),
                document.getIssuedAt(),
                document.getSentAt(),
                document.getPaidAt(),
                document.getCustomerReference(),
                document.getPoNumber(),
                document.getProjectCode(),
                document.getSalespersonCode(),
                document.getDeliveryAddressJson(),
                document.getLanguage(),
                document.getExchangeRate(),
                document.getPaymentReference(),
                document.getPaymentInstructions(),
                document.getBankDetailsJson(),
                document.getMetadataJson(),
                allLines.stream().map(this::toLineResponse).toList(),
                discountRepository.findAllByDocumentOrderByIdAsc(document).stream().map(this::toDiscountResponse).toList(),
                taxRepository.findAllByDocumentOrderByIdAsc(document).stream().map(this::toTaxResponse).toList(),
                clauseRepository.findAllByDocumentOrderByDisplayOrderAscIdAsc(document).stream().map(this::toClauseResponse).toList(),
                advanceRepository.findByDocument(document).map(this::toAdvanceResponse).orElse(null),
                earlyPaymentDiscountRepository.findByDocument(document).map(this::toEarlyPaymentDiscountResponse).orElse(null),
                signatureRepository.findFirstByDocumentOrderByCreatedAtDesc(document).map(this::toSignatureResponse).orElse(null),
                document.getInternalNotes(),
                recoverableRepository.findAllByDocumentOrderByCreatedAtAsc(document)
                        .stream().map(this::toRecoverableResponse).toList(),
                document.getLocked(),
                document.getFiscalNumber(),
                document.getFiscalDate(),
                document.getValidatedAt(),
                document.getSellerName(),
                document.getSellerNiu(),
                document.getSellerPhone(),
                document.getSellerEmail(),
                document.getCustomerNiu(),
                document.getCustomerCategory(),
                document.getPreviousHash(),
                document.getCurrentHash(),
                document.getFiscalSignature(),
                document.getSignedAt(),
                document.getOriginalDocumentNumber(),
                document.getOriginalDocumentType(),
                document.getCreditNoteReason()
        );
    }

    @Override
    public BillingDocumentLineResponse toLineResponse(BillingDocumentLine line) {
        return new BillingDocumentLineResponse(
                line.getLineOrder(),
                line.getLineType(),
                line.getItemCode(),
                line.getDescription(),
                line.getDetailedDescription(),
                line.getQuantity(),
                line.getUnit(),
                line.getUnitPrice(),
                line.getDiscountRate(),
                line.getDiscountAmount(),
                line.getTaxable(),
                line.getTaxIncluded(),
                line.getVatRate(),
                line.getAdditionalCentRate(),
                line.getSubtotalAmount(),
                line.getTaxableAmount(),
                line.getVatAmount(),
                line.getAdditionalCentAmount(),
                line.getTaxAmount(),
                line.getTotalAmount(),
                line.getSourceType(),
                line.getSourceCode(),
                line.getExternalReference(),
                line.getNotes(),
                line.getOptional()
        );
    }

    private EarlyPaymentDiscountResponse toEarlyPaymentDiscountResponse(BillingDocumentEarlyPaymentDiscount epd) {
        return new EarlyPaymentDiscountResponse(
                epd.getDiscountRate(),
                epd.getIfPaidBefore(),
                epd.getComputedAmount(),
                epd.getLabel()
        );
    }

    private BillingDocumentAdvanceResponse toAdvanceResponse(BillingDocumentAdvance advance) {
        return new BillingDocumentAdvanceResponse(
                advance.getAdvanceType(),
                advance.getAdvanceValue(),
                advance.getComputedAmount(),
                deserializeOrders(advance.getIncludedLineOrders()),
                deserializeOrders(advance.getExcludedLineOrders()),
                advance.getPaymentReference(),
                advance.getReferenceLabel(),
                advance.getDueDate(),
                advance.getStatus(),
                advance.getPaidAt(),
                advance.getNotes()
        );
    }

    private BillingDocumentSignatureResponse toSignatureResponse(BillingDocumentSignature sig) {
        return new BillingDocumentSignatureResponse(
                sig.getSignerName(),
                sig.getSignerEmail(),
                sig.getStatus(),
                sig.getSignedAt(),
                sig.getExpiresAt()
        );
    }

    private List<Integer> deserializeOrders(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
    }

    private BillingRecoverableResponse toRecoverableResponse(com.sni.bokaticowork.features.billing.model.BillingDocumentRecoverable r) {
        return new BillingRecoverableResponse(
                r.getRecoverableNumber(),
                r.getDocument().getDocumentNumber(),
                r.getItemDescription(),
                r.getQuantity(),
                r.getUnit(),
                r.getSourceType(),
                r.getSourceCode(),
                r.getStatus(),
                r.getNotes(),
                r.getRecoveredAt(),
                r.getRecoveredBy(),
                r.getCreatedAt()
        );
    }

    @Override
    public BillingDocumentDiscountResponse toDiscountResponse(BillingDocumentDiscount discount) {
        return new BillingDocumentDiscountResponse(
                discount.getDiscountCode(),
                discount.getDescription(),
                discount.getDiscountType(),
                discount.getValue(),
                discount.getAmount()
        );
    }

    @Override
    public BillingDocumentTaxResponse toTaxResponse(BillingDocumentTax tax) {
        return new BillingDocumentTaxResponse(
                tax.getTaxCode(),
                tax.getTaxName(),
                tax.getRate(),
                tax.getTaxableAmount(),
                tax.getTaxAmount()
        );
    }

    @Override
    public BillingDocumentClauseResponse toClauseResponse(BillingDocumentClause clause) {
        return new BillingDocumentClauseResponse(
                clause.getClauseCode(),
                clause.getTitle(),
                clause.getBody(),
                clause.getDisplayOrder()
        );
    }
}
