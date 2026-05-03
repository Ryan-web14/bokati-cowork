package com.sni.bokaticowork.features.billing.mapper.decorator;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentClauseResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentDiscountResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentTaxResponse;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentClause;
import com.sni.bokaticowork.features.billing.model.BillingDocumentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingDocumentTax;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentClauseRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentTaxRepository;
import com.sni.bokaticowork.features.payment.service.support.TransactionContextResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

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
    private TransactionContextResolver contextResolver;

    @Override
    public BillingDocumentResponse toResponse(BillingDocument document) {
        TransactionContextResolver.PartyView party = contextResolver.resolveParty(document.getCustomerType(), document.getCustomerCode());
        TransactionContextResolver.SourceView source = contextResolver.resolveBillingDocumentSource(document);
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
                document.getIssueDate(),
                document.getDueDate(),
                document.getIssuedAt(),
                document.getSentAt(),
                document.getPaidAt(),
                document.getMetadataJson(),
                lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document).stream().map(this::toLineResponse).toList(),
                discountRepository.findAllByDocumentOrderByIdAsc(document).stream().map(this::toDiscountResponse).toList(),
                taxRepository.findAllByDocumentOrderByIdAsc(document).stream().map(this::toTaxResponse).toList(),
                clauseRepository.findAllByDocumentOrderByDisplayOrderAscIdAsc(document).stream().map(this::toClauseResponse).toList()
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
                line.getUnitPrice(),
                line.getDiscountRate(),
                line.getDiscountAmount(),
                line.getTaxable(),
                line.getVatRate(),
                line.getAdditionalCentRate(),
                line.getSubtotalAmount(),
                line.getTaxableAmount(),
                line.getVatAmount(),
                line.getAdditionalCentAmount(),
                line.getTaxAmount(),
                line.getTotalAmount(),
                line.getSourceType(),
                line.getSourceCode()
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
