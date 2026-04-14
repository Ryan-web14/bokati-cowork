package com.sni.bokaticowork.core.baseClasses.mapper.interfaces;


import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.dto.response.AddressResponse;
import com.sni.bokaticowork.core.baseClasses.mapper.decorator.AddressMapperDecorator;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.model.Country;
import org.mapstruct.*;



@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE,
        componentModel = "spring")
@DecoratedWith(AddressMapperDecorator.class)
public interface AddressMapper {

    @Named("setStringCountry")
    default String setStringCountry(Country obj){
        return obj.getName();
    }

    @Mapping(target = "country", qualifiedByName = "setStringCountry" )
    @Mapping(target = "fullAddress", ignore = true)
    AddressResponse toDTO(Address obj);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "country", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Address toEntity(AddressRequest request);

    @Mapping(target = "streetNumber", source = "request.streetNumber")
    @Mapping(target = "streetName", source = "request.streetName")
    @Mapping(target = "district", source = "request.district")
    @Mapping(target = "city", source = "request.city")
    Address mapAddressUpdate(Address address, AddressRequest request);

}
