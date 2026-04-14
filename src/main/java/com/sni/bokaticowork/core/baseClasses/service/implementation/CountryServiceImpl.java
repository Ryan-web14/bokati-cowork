package com.sni.bokaticowork.core.baseClasses.service.implementation;


import com.sni.bokaticowork.core.baseClasses.dto.request.CountryRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CountryResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.CountryMapper;
import com.sni.bokaticowork.core.baseClasses.model.Country;
import com.sni.bokaticowork.core.baseClasses.repository.CountryRepository;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CountryService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@RequiredArgsConstructor
@Service
@Slf4j
public class CountryServiceImpl implements CountryService {

    private final CountryMapper countryMapper;
    private final CountryRepository countryRepo;

    @Transactional
    @Override
    public void addCountry(CountryRequest request) {
        log.debug("Adding country with code {}",request.getCountryCode());
        log.debug("Mapping country request");

        try {

            if(!ValidationUtils.validateString(request.getCountryCode())
                    || !ValidationUtils.validateString(request.getName())
                    || !ValidationUtils.validateString(request.getPhoneCode())){
                throw new IllegalArgumentException("Invalid country request");
            }

            String countryCode = request.getCountryCode().toUpperCase();
            String countryName = request.getName().trim();

            if(countryRepo.existsByCountryCode(countryCode)){
                log.debug("Country with code {} already exist", request.getCountryCode());
                throw new ResourceAlreadyExistException("Country with code " + request.getCountryCode() + " already exist");
            }

            if(countryRepo.existsByCountryName(countryName)){
                log.debug("Country with name {} already exist", request.getName());
                throw new ResourceAlreadyExistException("Country with name " + request.getName() + " already exist");
            }

            if(countryRepo.existByPhoneCode(request.getPhoneCode())){
                log.debug("Phone code {} already exist", request.getPhoneCode());
                throw new ResourceAlreadyExistException("Phone code " + request.getPhoneCode() + " already exist");
            }
        }catch (ResourceAlreadyExistException ex){
            log.error("{} already exist", ex.getMessage());
            throw new BadRequestException("Error with creation of the country", ex);
        }catch(IllegalArgumentException ex){
            log.error("Invalid country request");
            throw new BadRequestException("Invalid country request", ex);
        }
        Country country = countryMapper.toEntity(request);
        countryRepo.save(country);
        return;
    }

    @Transactional
    @Override
    public CountryResponse updateCountry(String countryCode, @NotNull CountryRequest request) {

       Country country;

        try {
            country = countryRepo.findByCountryCode(countryCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Country with code " + countryCode + " not found"));
            log.debug("Updating country with code {}",request.getCountryCode());

            if(!ValidationUtils.validateString(request.getCountryCode())
                    || !ValidationUtils.validateString(request.getName())
                    || !ValidationUtils.validateString(request.getPhoneCode())){
                throw new IllegalArgumentException("Invalid country request");
            }

            String requestedCountryCode = request.getCountryCode().toUpperCase();
            String requestedCountryName = request.getName().trim();

            if(countryRepo.existsByCountryCodeAndIdNot(requestedCountryCode, country.getId())){
                log.debug("Update request: Country with code {} already exist", request.getCountryCode());
                throw new ResourceAlreadyExistException("Country with code " + request.getCountryCode() + " already exist");
            }

            if(countryRepo.existsByNameIgnoreCaseAndIdNot(requestedCountryName, country.getId())){
                log.debug("Update request: Country with name {} already exist", request.getName());
                throw new ResourceAlreadyExistException("Country with name " + request.getName() + " already exist");
            }

            if(countryRepo.existsByPhoneCodeAndIdNot(request.getPhoneCode(), country.getId())){
                log.debug("Update request: Phone code {} already exist", request.getPhoneCode());
                throw new ResourceAlreadyExistException("Phone code " + request.getPhoneCode() + " already exist");
            }
        }catch (ResourceAlreadyExistException ex){
            log.error("Update request: Country with code {} already exist", request.getCountryCode() != null ? request.getCountryCode() : "null");
            throw new BadRequestException(ex.getMessage(), ex);
        }catch(IllegalArgumentException ex){
            log.error("Update request: Invalid country request");
            throw new BadRequestException("Invalid country request", ex);
        }

        return countryMapper.toDTO(countryRepo.save(
                countryMapper.mapCountryUpdate(country, request)));
    }

    @Transactional(readOnly = true)
    @Override
    public CountryResponse getCountryByCode(@NotNull String code){
        Country country = countryRepo.findByCountryCode(code.toUpperCase())
                .orElseThrow(()->new ResourceNotFoundException("Country with code " + code + " not found"));

        return countryMapper.toDTO(country);
    }

    @Override
    public void deleteCountry(String countryCode){
       Country country = countryRepo.findByCountryCode(countryCode.toUpperCase())
               .orElseThrow(()->new ResourceNotFoundException("Country with code " + countryCode + " not found"));
       countryRepo.delete(country);
    }

    @Override
    public void softDelete(String countryCode){
        Country country = countryRepo.findByCountryCode(countryCode.toUpperCase())
                .orElseThrow(()->new ResourceNotFoundException("Country with code " + countryCode + " not found"));
        countryRepo.softDeleteByCountryCode(country.getCountryCode());
        return;
    }

    @Transactional(readOnly = true)
    @Override
    public CountryResponse getCountryByPhoneCode(String phoneCode){

        Country country = countryRepo.findCountryByPhoneCode(phoneCode)
                .orElseThrow(()->new ResourceNotFoundException("Country with phone code " + phoneCode + " not found"));
        return countryMapper.toDTO(country);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CountryResponse> getAllCountries(){

        return countryRepo.findAllByOrderByNameAsc().stream()
                .map(countryMapper::toDTO)
                .toList();
    }

}




