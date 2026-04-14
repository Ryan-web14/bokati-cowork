package com.sni.bokaticowork.core.settings.userSettings.dto.request;

import jakarta.validation.constraints.Min;

public record WalletPreferencesRequest(
           Boolean autoTopupEnabled,
           @Min(0) Integer autoTopupThreshold,
           @Min(0) Integer autoTopupAmount,
           @Min(0) Integer spendingDailyLimit,
           @Min(0) Integer spendingWeeklyLimit) {

}
