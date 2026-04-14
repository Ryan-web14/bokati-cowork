package com.sni.bokaticowork.core.settings.userSettings.dto.response;

import jakarta.validation.constraints.Min;

public record WalletPreferencesResponse(
        Boolean autoTopupEnabled,
        Integer autoTopupThreshold,
        Integer autoTopupAmount,
        Integer spendingDailyLimit,
        Integer spendingWeeklyLimit
) {
}
