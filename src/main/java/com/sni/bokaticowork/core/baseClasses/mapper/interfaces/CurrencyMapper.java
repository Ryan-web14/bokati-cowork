package com.sni.bokaticowork.core.baseClasses.mapper.interfaces;



import com.sni.bokaticowork.core.baseClasses.dto.request.CurrencyRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CurrencyResponse;
import com.sni.bokaticowork.core.baseClasses.model.Currency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE,
        componentModel = "spring")
public interface CurrencyMapper {

    public CurrencyResponse toDTO(Currency obj);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currencyName", expression = "java(request.getCurrencyName().toUpperCase())")
    @Mapping(target = "currencyCode", expression = "java(request.getCurrencyCode().toUpperCase())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public Currency toEntity(CurrencyRequest request);

}
