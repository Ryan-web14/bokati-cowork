package com.sni.bokaticowork.core.baseClasses.service.interfaces;



import com.sni.bokaticowork.core.baseClasses.dto.request.CountryRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CountryResponse;

import java.util.List;

public interface CountryService {

    void addCountry(CountryRequest request);
    CountryResponse updateCountry(String countryCode, CountryRequest request);
    void deleteCountry(String countryCode);
    void softDelete(String countryCode);
    CountryResponse getCountryByPhoneCode(String phoneCode);
//  public CountryResponse getCountryByName(String name);
CountryResponse getCountryByCode(String code);
    List<CountryResponse> getAllCountries();
}
