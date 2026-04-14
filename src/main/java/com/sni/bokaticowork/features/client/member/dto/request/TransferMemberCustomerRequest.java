package com.sni.bokaticowork.features.client.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TransferMemberCustomerRequest(
        @NotBlank String customerId
) {
}
