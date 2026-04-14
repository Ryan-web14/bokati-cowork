package com.sni.bokaticowork.core.settings.userSettings.dto.request;


import com.sni.bokaticowork.core.settings.userSettings.model.NotificationPreferences;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdateMemberSettingsRequest {

    private String langage;
    private String timeZone;
    private Boolean showNameOnDisplay;
    private Boolean showEmail;
    private Boolean showPhone;
    private BillingPreferences billingPref;
    private WalletPreferencesRequest walletPref;
    private List<NotificationPreferences> notificationPref;

}
