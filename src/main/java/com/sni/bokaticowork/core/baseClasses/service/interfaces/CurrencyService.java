package com.sni.bokaticowork.core.baseClasses.service.interfaces;


import com.sni.bokaticowork.core.baseClasses.dto.request.CurrencyRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CurrencyResponse;
import com.sni.bokaticowork.core.baseClasses.model.Currency;

import java.util.List;

public interface CurrencyService {

    CurrencyResponse  createCurrency(CurrencyRequest request);
    void deleteCurrency(String currencyCode);
    CurrencyResponse updateCurrency(String currencyCode, CurrencyRequest request);
    CurrencyResponse getCurrencyByCode(String code);
    List<CurrencyResponse> getAllCurrency();

    //Method to use between services
    Currency serviceCurrencyByCode(String code);

}
