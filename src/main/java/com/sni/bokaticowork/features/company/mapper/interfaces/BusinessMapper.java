package com.sni.bokaticowork.features.company.mapper.interfaces;

import com.sni.bokaticowork.features.company.dto.request.BusinessEntityRequest;
import com.sni.bokaticowork.features.company.dto.response.BusinessEntityResponse;
import com.sni.bokaticowork.features.company.mapper.decorator.BusinessMapperDecorator;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE,
        componentModel = "spring")
@DecoratedWith(BusinessMapperDecorator.class)
public interface BusinessMapper {
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "baseCurrencyCode", ignore = true)
    BusinessEntityResponse toDTO(BusinessEntity obj);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "baseCurrency", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "created_by", ignore = true)
    @Mapping(target = "updated_by", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BusinessEntity toEntity(BusinessEntityRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "baseCurrency", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "created_by", ignore = true)
    @Mapping(target = "updated_by", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    BusinessEntity mapUpdateRequestToBusiness(@MappingTarget BusinessEntity business, BusinessEntityRequest request);

}
