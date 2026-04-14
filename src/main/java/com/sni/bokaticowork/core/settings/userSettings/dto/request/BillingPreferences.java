package com.sni.bokaticowork.core.settings.userSettings.dto.request;

import com.sni.bokaticowork.core.enums.BillingEntityType;
import jakarta.validation.constraints.Size;

public record BillingPreferences(
        BillingEntityType billingEntityType,
        @Size(max = 255) String billingCompanyName,
        @Size(max = 255) String billingEmail,
        @Size(max = 255)String billlingAddress) {
}
