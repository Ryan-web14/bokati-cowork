package com.sni.bokaticowork.core.settings.userSettings.mapper.interfaces;

import com.sni.bokaticowork.core.settings.userSettings.dto.request.BillingPreferences;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.BillingPreferencesResponse;
import org.mapstruct.Mapper;

@Mapper(unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
componentModel = "spring")
public interface BillingPreferenceMapper {


    BillingPreferencesResponse toDto(BillingPreferences obj);

    BillingPreferences toEntity (BillingPreferences request);

}

