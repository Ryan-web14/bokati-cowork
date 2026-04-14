package com.sni.bokaticowork.core.baseClasses.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AddressResponse {

    private String streetNumber;
    private String streetName;
    private String district;
    private String city;
    private String country;
    private String fullAddress;
}
