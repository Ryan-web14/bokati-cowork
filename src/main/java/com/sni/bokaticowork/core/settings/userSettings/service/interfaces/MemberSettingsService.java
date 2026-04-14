package com.sni.bokaticowork.core.settings.userSettings.service.interfaces;

import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateMemberSettingsRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateNotificationPreferenceRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.MemberSettingsResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.NotificationPreferenceResponse;

import java.util.List;

public interface MemberSettingsService {

    MemberSettingsResponse getSettings(String memberId);

    MemberSettingsResponse updateSettings(String memberId,
                                          UpdateMemberSettingsRequest request,
                                          Long actorId,
                                          String reason);

    List<NotificationPreferenceResponse> getNotificationPreferences(String memberId);

    List<NotificationPreferenceResponse> updateNotificationPreference(String memberId,
                                                                      UpdateNotificationPreferenceRequest request,
                                                                      Long actorId,
                                                                      String reason);
}
