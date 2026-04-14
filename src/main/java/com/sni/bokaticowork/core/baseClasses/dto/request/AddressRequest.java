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
public class AddressRequest {

    private String streetNumber;
    private String streetName;
    private String district;
    private String city;

    @Size(max = 3)
    private String countryCode;

}