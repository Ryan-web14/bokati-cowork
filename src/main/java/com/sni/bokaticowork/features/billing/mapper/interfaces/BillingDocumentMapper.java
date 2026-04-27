package com.sni.bokaticowork.features.billing.mapper.interfaces;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentClauseResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentDiscountResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentTaxResponse;
import com.sni.bokaticowork.features.billing.mapper.decorator.BillingDocumentMapperDecorator;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentClause;
import com.sni.bokaticowork.features.billing.model.BillingDocumentDiscount;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingDocumentTax;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(BillingDocumentMapperDecorator.class)
public interface BillingDocumentMapper {

    default BillingDocumentResponse toResponse(BillingDocument document) {
        return null;
    }

    default BillingDocumentLineResponse toLineResponse(BillingDocumentLine line) {
        return null;
    }

    default BillingDocumentDiscountResponse toDiscountResponse(BillingDocumentDiscount discount) {
        return null;
    }

    default BillingDocumentTaxResponse toTaxResponse(BillingDocumentTax tax) {
        return null;
    }

    default BillingDocumentClauseResponse toClauseResponse(BillingDocumentClause clause) {
        return null;
    }
}
