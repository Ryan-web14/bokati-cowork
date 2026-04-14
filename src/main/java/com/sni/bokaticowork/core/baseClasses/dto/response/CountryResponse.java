package com.sni.bokaticowork.core.baseClasses.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CountryResponse {

    private String name;
    private String countryCode;
    private String phoneCode;
    private CurrencyResponse defaultCurrency;
    private boolean isOhadaMember;
}
