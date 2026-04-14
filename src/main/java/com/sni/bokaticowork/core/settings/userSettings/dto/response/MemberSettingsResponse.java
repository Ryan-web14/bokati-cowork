package com.sni.bokaticowork.core.settings.userSettings.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MemberSettingsResponse {
    private String langage;
    private String timeZone;
    private Boolean showNameOnDisplay;
    private Boolean showEmail;
    private Boolean showPhone;
    private BillingPreferencesResponse billingPref;
    private WalletPreferencesResponse walletPref;
    private List<NotificationPreferenceResponse> notificationPref;

}
