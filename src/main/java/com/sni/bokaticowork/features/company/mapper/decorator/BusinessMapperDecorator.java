package com.sni.bokaticowork.features.company.mapper.decorator;


import com.sni.bokaticowork.features.company.dto.request.BusinessEntityRequest;
import com.sni.bokaticowork.features.company.dto.response.BusinessEntityResponse;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.AddressMapper;
import com.sni.bokaticowork.features.company.mapper.interfaces.BusinessMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public abstract class BusinessMapperDecorator implements BusinessMapper {

    @Autowired
    @Qualifier("delegate")
    private  BusinessMapper delegate;

    private AddressMapper addressMapper;


    public BusinessMapperDecorator(){}

    @Autowired
    private void setAddressMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public BusinessEntityResponse toDTO(BusinessEntity obj){
        BusinessEntityResponse response = delegate.toDTO(obj);
        response.setAddress(obj.getAddress() == null ? null : addressMapper.toDTO(obj.getAddress()));
        response.setStatus(obj.getStatus() == null ? null : obj.getStatus().name());
        response.setBaseCurrencyCode(obj.getBaseCurrency() == null ? null : obj.getBaseCurrency().getCurrencyCode());
        return response;
    }

    @Override
    public BusinessEntity toEntity(BusinessEntityRequest request){
        BusinessEntity business = delegate.toEntity(request);
        business.setName(normalize(request.getName()));
        business.setLegalForm(normalizeUpper(request.getLegalForm()));
        business.setNiuNumber(normalizeUpper(request.getNiuNumber()));
        business.setRccmNumber(normalizeUpper(request.getRccmNumber()));
        business.setTaxId(normalize(request.getTaxId()));
        business.setActivity(normalize(request.getActivity()));
        business.setPhone(normalizePhone(request.getPhone()));
        business.setEmail(normalizeLower(request.getEmail()));
        return business;
    }

    @Override
    public BusinessEntity mapUpdateRequestToBusiness(BusinessEntity business, BusinessEntityRequest request){
        delegate.mapUpdateRequestToBusiness(business, request);

        if (request.getName() != null) {
            business.setName(normalize(request.getName()));
        }
        if (request.getLegalForm() != null) {
            business.setLegalForm(normalizeUpper(request.getLegalForm()));
        }
        if (request.getNiuNumber() != null) {
            business.setNiuNumber(normalizeUpper(request.getNiuNumber()));
        }
        if (request.getRccmNumber() != null) {
            business.setRccmNumber(normalizeUpper(request.getRccmNumber()));
        }
        if (request.getTaxId() != null) {
            business.setTaxId(normalize(request.getTaxId()));
        }
        if (request.getActivity() != null) {
            business.setActivity(normalize(request.getActivity()));
        }
        if (request.getPhone() != null) {
            business.setPhone(normalizePhone(request.getPhone()));
        }
        if (request.getEmail() != null) {
            business.setEmail(normalizeLower(request.getEmail()));
        }
        return business;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeLower(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        return value == null ? null : value.replaceAll("\\s+", "");
    }
}
