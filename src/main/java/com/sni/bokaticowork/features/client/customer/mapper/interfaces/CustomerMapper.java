package com.sni.bokaticowork.features.client.customer.mapper.interfaces;

import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerResponse;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerSummaryresponse;
import com.sni.bokaticowork.features.client.customer.mapper.decorator.CustomerMapperDecorator;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import org.mapstruct.*;

@Mapper(unmappedSourcePolicy = ReportingPolicy.IGNORE ,
        componentModel = "spring")
@DecoratedWith(CustomerMapperDecorator.class)
public interface CustomerMapper {

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "address", ignore = true)
    CustomerResponse toDto(Customer obj);

    @Mapping(target = "type", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "isMember", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Customer toEntity(CustomerRequest response);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "isMember", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "firstname", source = "request.firstname")
    @Mapping(target = "lastname", source = "request.lastname")
    @Mapping(target = "companyName", source = "request.companyName")
    @Mapping(target = "email", source = "request.email")
    @Mapping(target = "phone", source = "request.phone")
    @Mapping(target = "whatsappPhone", source = "request.whatsappPhone")
    @Mapping(target = "billingEmail", source = "request.billingEmail")
    Customer updateEntity(Customer customer, CustomerRequest request);

    default CustomerSummaryresponse toSummary(Customer customer){

        String name = customer.getType() == CustomerType.COMPANY ?
                customer.getCompanyName() : customer.getFirstname() + " " + customer.getLastname();

        return CustomerSummaryresponse.builder()
                .id(customer.getId())
                .customerId(customer.getCustomerId())
                .displayName(name)
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .status(customer.getStatus().name())
                .build();
    }

}
