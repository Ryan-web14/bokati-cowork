package com.sni.bokaticowork.core.baseClasses.service.implementation;


import com.sni.bokaticowork.core.baseClasses.dto.request.CurrencyRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CurrencyResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.CurrencyMapper;
import com.sni.bokaticowork.core.baseClasses.model.Currency;
import com.sni.bokaticowork.core.baseClasses.repository.CurrencyRepository;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CurrencyService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@RequiredArgsConstructor
@Service
@Slf4j
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyMapper currencyMapper;
    private final CurrencyRepository currencyRepo;

    @Transactional
    @Override
    public CurrencyResponse createCurrency(CurrencyRequest request){

        log.info("Adding currency with code {}",request.getCurrencyCode());

        Currency currency = new Currency();
        try{

            if(!ValidationUtils.validateString(request.getCurrencyName())
                    || !ValidationUtils.validateString(request.getCurrencyCode())){
                throw new IllegalArgumentException("Invalid currency request");
            }

            String currencyCode = request.getCurrencyCode().toUpperCase();
            String currencyName = request.getCurrencyName().trim();

            if(currencyRepo.existsByCurrencyCode(currencyCode)){
                log.debug("Currency with code {} already exist", request.getCurrencyCode());
                throw new ResourceAlreadyExistException("Currency with code " + request.getCurrencyCode() + " already exist");
            }

            if(currencyRepo.existsByCurrencyName(currencyName)){
                log.debug("Currency with name {} already exist", request.getCurrencyName());
                throw new ResourceAlreadyExistException("Currency with name " + request.getCurrencyName() + " already exist");
            }

            currency = currencyMapper.toEntity(request);
            log.debug("Mapping currency request");
        }catch (ResourceAlreadyExistException ex){
            log.error("Currency with code {} already exist", request.getCurrencyCode() != null ? request.getCurrencyCode() : "null");
            throw new BadRequestException("Currency with code " + request.getCurrencyCode() + " already exist", ex);
        }catch (IllegalArgumentException ex){
            log.error("Invalid currency request");
            throw new BadRequestException("Invalid currency request", ex);
        }

        return currencyMapper.toDTO(currencyRepo.save(currency));
    }

    @Override
    public void deleteCurrency(String currencyCode){
        log.debug("Deleting currency with code {}", currencyCode);
        currencyRepo.deleteByCurrencyCode(currencyCode.toUpperCase());
        return;
    }

    @Transactional
    @Override
    public CurrencyResponse updateCurrency(String currencyCode, CurrencyRequest request){

        Currency currency;

        try{
             currency = currencyRepo.findByCurrencyCode(currencyCode.toUpperCase())
                    .orElseThrow(()->new ResourceNotFoundException("Currency with code " + currencyCode + " not found"));

            if(!ValidationUtils.validateString(request.getCurrencyName())
                    || !ValidationUtils.validateString(request.getCurrencyCode())){
                throw new IllegalArgumentException("Invalid currency update request");
            }

            String requestedCode = request.getCurrencyCode().toUpperCase();
            String requestedName = request.getCurrencyName().trim();

            if(currencyRepo.existsByCurrencyCodeAndIdNot(requestedCode, currency.getId())){
                log.debug("Update request: Currency with code {} already exist", request.getCurrencyCode());
                throw new ResourceAlreadyExistException("Currency with code " + request.getCurrencyCode() + " already exist");
            }

            if(currencyRepo.existsByCurrencyNameIgnoreCaseAndIdNot(requestedName, currency.getId())){
                log.debug("Update request: Currency with name {} already exist", request.getCurrencyName());
                throw new ResourceAlreadyExistException("Currency with name " + request.getCurrencyName() + " already exist");
            }

            currency.setCurrencyName(requestedName.toUpperCase());
            currency.setCurrencyCode(requestedCode);

        }catch (ResourceAlreadyExistException ex){
            log.error("Update request:  Currency with code {} already exist", request.getCurrencyCode() != null ? request.getCurrencyCode() : "null");
            throw new BadRequestException(ex.getMessage(), ex);
        }catch (IllegalArgumentException ex){
            log.error("Invalid currency update request");
            throw new BadRequestException("Invalid currency update request", ex);
        }catch(ResourceNotFoundException ex){
            log.error("Currency with code {} not found", request.getCurrencyCode() != null ? request.getCurrencyCode() : "null");
            throw new BadRequestException("Currency with code " + request.getCurrencyCode() + " not found", ex);
        }

        return currencyMapper.toDTO(currencyRepo.save(currency));
    }

    @Transactional(readOnly = true)
    public CurrencyResponse getCurrencyByCode(String code){
        Currency currency = currencyRepo.findByCurrencyCode(code.toUpperCase())
                .orElseThrow(()->new ResourceNotFoundException("Currency with code " + code + " not found"));

        return currencyMapper.toDTO(currency);
    }

    @Transactional(readOnly = true)
    public List<CurrencyResponse> getAllCurrency(){

        return currencyRepo.findAllByOrderByCurrencyNameAsc().stream()
                .map(currencyMapper::toDTO)
                .toList();
    }

    @Override
    public Currency serviceCurrencyByCode(String code) {
        return currencyRepo.findByCurrencyCode(code.toUpperCase())
                .orElseThrow(()->new ResourceNotFoundException("Currency with code " + code + " not found"));
    }

}

