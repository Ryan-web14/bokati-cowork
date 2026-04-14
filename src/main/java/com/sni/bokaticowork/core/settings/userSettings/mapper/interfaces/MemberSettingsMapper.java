package com.sni.bokaticowork.core.settings.userSettings.mapper.interfaces;

import com.sni.bokaticowork.core.settings.userSettings.dto.request.BillingPreferences;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateMemberSettingsRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.WalletPreferencesRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.BillingPreferencesResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.MemberSettingsResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.NotificationPreferenceResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.WalletPreferencesResponse;
import com.sni.bokaticowork.core.settings.userSettings.model.MemberSettings;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface MemberSettingsMapper {

    default MemberSettingsResponse toDto(MemberSettings obj) {
        if (obj == null) {
            return null;
        }

        return MemberSettingsResponse.builder()
                .langage(obj.getLangage())
                .timeZone(obj.getTimeZone())
                .showNameOnDisplay(obj.isShowNameOnDisplays())
                .showEmail(obj.isShowEmail())
                .showPhone(obj.isShowPhone())
                .billingPref(toBillingPreferencesResponse(obj))
                .walletPref(toWalletPreferencesResponse(obj))
                .notificationPref(java.util.List.<NotificationPreferenceResponse>of())
                .build();
    }

    default MemberSettings toEntity(UpdateMemberSettingsRequest request) {
        if (request == null) {
            return null;
        }

        MemberSettings settings = new MemberSettings();
        settings.setLangage(request.getLangage());
        settings.setTimeZone(request.getTimeZone());
        settings.setShowNameOnDisplays(Boolean.TRUE.equals(request.getShowNameOnDisplay()));
        settings.setShowEmail(Boolean.TRUE.equals(request.getShowEmail()));
        settings.setShowPhone(Boolean.TRUE.equals(request.getShowPhone()));

        BillingPreferences billing = request.getBillingPref();
        if (billing != null) {
            settings.setEntityType(billing.billingEntityType());
            settings.setBillingCompanyName(billing.billingCompanyName());
            settings.setBillingEmail(billing.billingEmail());
            settings.setBillingAddress(billing.billlingAddress());
        }

        WalletPreferencesRequest wallet = request.getWalletPref();
        if (wallet != null) {
            settings.setWalletAutoPopUp(Boolean.TRUE.equals(wallet.autoTopupEnabled()));
            settings.setWalletAutoTopupThreshold(wallet.autoTopupThreshold());
            settings.setWalletAutoTopupAmount(wallet.autoTopupAmount());
            settings.setWalletSpendingDailyLimit(wallet.spendingDailyLimit());
            settings.setWalletSpendingWeeklyLimit(wallet.spendingWeeklyLimit());
        }

        return settings;
    }

    private BillingPreferencesResponse toBillingPreferencesResponse(MemberSettings obj) {
        return new BillingPreferencesResponse(
                obj.getEntityType() == null ? null : obj.getEntityType().name(),
                obj.getBillingCompanyName(),
                obj.getBillingEmail(),
                obj.getBillingAddress()
        );
    }

    private WalletPreferencesResponse toWalletPreferencesResponse(MemberSettings obj) {
        return new WalletPreferencesResponse(
                obj.isWalletAutoPopUp(),
                obj.getWalletAutoTopupThreshold(),
                obj.getWalletAutoTopupAmount(),
                obj.getWalletSpendingDailyLimit(),
                obj.getWalletSpendingWeeklyLimit()
        );
    }
}
