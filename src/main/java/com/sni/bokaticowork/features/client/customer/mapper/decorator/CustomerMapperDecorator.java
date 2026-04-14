package com.sni.bokaticowork.features.client.customer.mapper.decorator;


import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerResponse;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.mapper.interfaces.CustomerMapper;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.mapper.interfaces.AddressMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.format.Normalization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Component
@Slf4j
public abstract class CustomerMapperDecorator implements CustomerMapper {

    @Autowired
    @Qualifier("delegate")
    private CustomerMapper customerMapper;
    private AddressMapper addressMapper;

    public CustomerMapperDecorator(){}

    @Autowired
    private void setAddressMapper(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public CustomerResponse toDto(Customer obj){

        CustomerResponse response = customerMapper.toDto(obj);
        response.setType(obj.getType().toString());
        response.setAddress(addressMapper.toDTO(obj.getAddress()));
        return response;
    }

    @Override
    public Customer toEntity(CustomerRequest request){

        Customer customer = customerMapper.toEntity(request);

        customer.setCompanyName(
                request.getCompanyName() != null ?
                        request.getCompanyName() : "No company"
        );

        customer.setFirstname(request.getFirstname() != null ?
                Normalization.normalizeFirstname(request.getFirstname()) : "No firstname");

        customer.setLastname(request.getLastname() != null ?
                Normalization.normalizeLastname(request.getLastname()) : "No lastname");

        customer.setType(parseCustomerType(request.getType()));
        customer.setAddress(addressMapper.toEntity(request.getAddress()));
        return customer;
    }

    @Override
    public Customer updateEntity(Customer customer, CustomerRequest request) {
        if (customer == null || request == null) {
            return customer;
        }

        boolean updated = false;

        if (hasText(request.getType())) {
            CustomerType requestedType = parseCustomerType(request.getType());
            if (customer.getType() != requestedType) {
                customer.setType(requestedType);
                updated = true;
            }
        }

        if (hasText(request.getFirstname()) && !isSameValue(customer.getFirstname(), request.getFirstname())) {
            customer.setFirstname(request.getFirstname().trim());
            updated = true;
        }

        if (hasText(request.getLastname()) && !isSameValue(customer.getLastname(), request.getLastname())) {
            customer.setLastname(request.getLastname().trim());
            updated = true;
        }

        if (hasText(request.getCompanyName()) && !isSameValue(customer.getCompanyName(), request.getCompanyName())) {
            customer.setCompanyName(request.getCompanyName().trim());
            updated = true;
        }

        if (hasText(request.getEmail()) && !isSameEmail(customer.getEmail(), request.getEmail())) {
            customer.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
            updated = true;
        }

        if (hasText(request.getBillingEmail()) && !isSameEmail(customer.getBillingEmail(), request.getBillingEmail())) {
            customer.setBillingEmail(request.getBillingEmail().trim().toLowerCase(Locale.ROOT));
            updated = true;
        }

        if (hasText(request.getPhone()) && !isSameValue(customer.getPhone(), request.getPhone())) {
            customer.setPhone(request.getPhone().trim());
            updated = true;
        }

        if (hasText(request.getWhatsappPhone()) && !isSameValue(customer.getWhatsappPhone(), request.getWhatsappPhone())) {
            customer.setWhatsappPhone(request.getWhatsappPhone().trim());
            updated = true;
        }

        if (request.getAddress() != null) {
            if (customer.getAddress() == null) {
                customer.setAddress(addressMapper.toEntity(request.getAddress()));
                updated = true;
            } else if (hasAddressChanges(customer, request.getAddress())) {
                AddressRequest mergedAddress = mergeAddressRequest(customer, request.getAddress());
                customer.setAddress(addressMapper.mapAddressUpdate(customer.getAddress(), mergedAddress));
                updated = true;
            }
        }

        if (updated && customer.getStatus() == null) {
            customer.setStatus(CustomerStatus.ACTIVE);
        }

        if (updated) {
            customer.setUpdatedAt(Instant.now());
        }

        return customer;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isSameValue(String current, String incoming) {
        return Objects.equals(
                current == null ? null : current.trim(),
                incoming == null ? null : incoming.trim()
        );
    }

    private static boolean isSameEmail(String current, String incoming) {
        if (current == null && incoming == null) {
            return true;
        }
        if (current == null || incoming == null) {
            return false;
        }
        return current.trim().equalsIgnoreCase(incoming.trim());
    }

    private static CustomerType parseCustomerType(String type) {
        try {
            return CustomerType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid customer type: " + type, ex);
        }
    }

    private AddressRequest mergeAddressRequest(Customer customer, AddressRequest requestAddress) {
        return AddressRequest.builder()
                .streetNumber(hasText(requestAddress.getStreetNumber()) ? requestAddress.getStreetNumber() : customer.getAddress().getStreetNumber())
                .streetName(hasText(requestAddress.getStreetName()) ? requestAddress.getStreetName() : customer.getAddress().getStreetName())
                .district(hasText(requestAddress.getDistrict()) ? requestAddress.getDistrict() : customer.getAddress().getDistrict())
                .city(hasText(requestAddress.getCity()) ? requestAddress.getCity() : customer.getAddress().getCity())
                .countryCode(hasText(requestAddress.getCountryCode()) ? requestAddress.getCountryCode() : customer.getAddress().getCountry().getCountryCode())
                .build();
    }

    private boolean hasAddressChanges(Customer customer, AddressRequest requestAddress) {
        if (hasText(requestAddress.getStreetNumber()) && !isSameValue(customer.getAddress().getStreetNumber(), requestAddress.getStreetNumber())) {
            return true;
        }
        if (hasText(requestAddress.getStreetName()) && !isSameValue(customer.getAddress().getStreetName(), requestAddress.getStreetName())) {
            return true;
        }
        if (hasText(requestAddress.getDistrict()) && !isSameValue(customer.getAddress().getDistrict(), requestAddress.getDistrict())) {
            return true;
        }
        if (hasText(requestAddress.getCity()) && !isSameValue(customer.getAddress().getCity(), requestAddress.getCity())) {
            return true;
        }
        return hasText(requestAddress.getCountryCode()) &&
                !isSameValue(customer.getAddress().getCountry().getCountryCode(), requestAddress.getCountryCode());
    }

}
