package com.sni.bokaticowork.features.billing.repository.specification.criteria;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;

import java.time.LocalDate;

public record BillingDocumentSearchCriteria(
        BillingDocumentType type,
        BillingDocumentStatus status,
        String customerType,
        String customerCode,
        String sourceType,
        String sourceCode,
        LocalDate fromDate,
        LocalDate toDate,
        String searchText
) {
    public String typeValue() {
        return type == null ? null : type.name();
    }

    public String statusValue() {
        return status == null ? null : status.name();
    }
}
