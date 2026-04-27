package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceFromBillableItemsRequest(
        String title,
        String description,
        LocalDate issueDate,
        LocalDate dueDate,
        @NotEmpty List<String> billableNumbers
) {
}
