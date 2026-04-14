package com.sni.bokaticowork.core.settings.userSettings.dto.response;


import jakarta.validation.constraints.Size;

public record BillingPreferencesResponse(
        String billingEntityType,
        @Size(max = 255) String billingCompanyName,
        @Size(max = 255) String billingEmail,
        @Size(max = 255)String billlingAddress
) {
}
