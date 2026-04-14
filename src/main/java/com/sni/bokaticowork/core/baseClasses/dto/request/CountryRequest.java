package com.sni.bokaticowork.core.baseClasses.dto.request;


import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CountryRequest {
    private String name;

    @Size(max = 3)
    private String countryCode;

    private String currencyCode;
    private String phoneCode;
    private Boolean isOhadaMember;
}
