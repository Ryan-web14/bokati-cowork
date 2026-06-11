package com.sni.bokaticowork.features.billing.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddRecoverableItemsRequest(
        @NotEmpty @Valid List<RecoverableItemRequest> items
) {
}
