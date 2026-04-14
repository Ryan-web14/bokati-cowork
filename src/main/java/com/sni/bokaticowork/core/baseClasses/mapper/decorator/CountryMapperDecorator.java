package com.sni.bokaticowork.core.baseClasses.mapper.decorator;


import com.sni.bokaticowork.core.baseClasses.dto.request.CountryRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CountryResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.CountryMapper;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.CurrencyMapper;
import com.sni.bokaticowork.core.baseClasses.model.Country;
import com.sni.bokaticowork.core.baseClasses.model.Currency;
import com.sni.bokaticowork.core.baseClasses.repository.CurrencyRepository;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public abstract class CountryMapperDecorator implements CountryMapper {

    @Autowired
    @Qualifier("delegate")
    private CountryMapper delegate;
    private CurrencyRepository currencyRepo;
    private CurrencyMapper currencyMapper;

    public CountryMapperDecorator(){}

    @Autowired
    public void setCurrencyRepository(CurrencyRepository currencyRepo) {
        this.currencyRepo = currencyRepo;
    }

    @Autowired
    public void setCurrencyMapper(CurrencyMapper currencyMapper) {
        this.currencyMapper = currencyMapper;
    }

    @Override
    public CountryResponse toDTO(Country obj){

        if(obj == null){
            return null;
        }

        log.debug("Mapping country {} and ID - {}", obj.getCountryCode(), obj.getId());
        CountryResponse response = delegate.toDTO(obj);

        if ( obj.getDefaultCurrency() != null ){
            log.debug("Mapping currency {} and ID - {}",
                    obj.getDefaultCurrency().getCurrencyCode(), obj.getDefaultCurrency().getId());

            try{
                response.setDefaultCurrency(currencyMapper.toDTO(obj.getDefaultCurrency()));
            }catch (ResourceNotFoundException ex){
                log.error("No currency found for country {} and currency code {}",
                        obj.getCountryCode(), obj.getDefaultCurrency().getCurrencyCode());
                throw new BadRequestException("No currency found for country " + obj.getCountryCode()
                        + " and currency code " + obj.getDefaultCurrency().getCurrencyCode(), ex);
            }
        }
        return response;
    }

    @Override
    public Country toEntity(CountryRequest request){

        if(request == null){
            return null;
        }

        Country obj = delegate.toEntity(request);
        log.debug("Mapping country request");

        validateCurrency(obj, request);
        return obj;
    }

    @Override
    public Country mapCountryUpdate(Country country, CountryRequest request){

        List<String> err = new ArrayList<>();
        if(country == null || request == null){
            return null;
        }

        log.debug("Mapping update country {} and ID - {}", country.getCountryCode(), country.getId());

        try {
            if(StringUtils.hasText(request.getName())){
                if(!country.getName().equals(request.getName())) {
                    country.setName(request.getName().toUpperCase());
                }
            }else{
                err.add("Name is required");

            }
            if(StringUtils.hasText(request.getCountryCode()) && request.getCountryCode().length() <= 3){
                if(!country.getCountryCode().equals(request.getCountryCode()) ) {
                    country.setCountryCode(request.getCountryCode().toUpperCase());
                }
            }else{
                err.add("Invalid country code provided");
            }
            if(request.getIsOhadaMember() != null){
                country.setIsOhadaMember(request.getIsOhadaMember());
            }

            if (!err.isEmpty()) {
                throw new ValidationException("Invalid country update", err);
            }

            validateCurrency(country, request);
            }catch (ValidationException ex){
                throw new BadRequestException("Invalid country update", ex);
        }
        return country;
        }

        //Todo : validate request in mapper
        private void validateRequest(CountryRequest request){

        }

    private void validateCurrency(Country country, CountryRequest request){
        if(request == null || !StringUtils.hasText(request.getCurrencyCode())){
            return;
        }
        if(request.getCurrencyCode() != null){
            try{
                Currency currency = currencyRepo.findByCurrencyCode(request.getCurrencyCode().toUpperCase())
                        .orElseThrow(()->new ResourceNotFoundException("Currency with code " +
                                request.getCurrencyCode() + " not found"));
                country.setDefaultCurrency(currency);
            }catch (ResourceNotFoundException ex){
                throw new BadRequestException("Invalid currency provided", ex);
            }
        }
    }
}


