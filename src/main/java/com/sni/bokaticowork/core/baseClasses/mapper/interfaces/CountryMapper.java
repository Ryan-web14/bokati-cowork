package com.sni.bokaticowork.core.baseClasses.mapper.interfaces;


import com.sni.bokaticowork.core.baseClasses.dto.request.CountryRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.CountryResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.decorator.CountryMapperDecorator;
import com.sni.bokaticowork.core.baseClasses.model.Country;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE,
        componentModel = "spring")
@DecoratedWith(CountryMapperDecorator.class)
public interface CountryMapper {

    CountryResponse toDTO(Country obj);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(request.getName().toUpperCase())")
    @Mapping(target = "countryCode", expression = "java(request.getCountryCode().toUpperCase())")
    @Mapping(target = "defaultCurrency", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Country toEntity(CountryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "request.name")
    @Mapping(target = "countryCode", source = "request.countryCode")
    @Mapping(target = "defaultCurrency", ignore = true)
    @Mapping(target = "isOhadaMember", source = "request.isOhadaMember")
    @Mapping(target = "phoneCode", source = "request.phoneCode")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Country mapCountryUpdate(Country country, CountryRequest request);
}
