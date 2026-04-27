package com.sni.bokaticowork.features.client.customer.service.implementation;

import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.dto.request.ChangeCustomerStatusRequest;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerResponse;
import com.sni.bokaticowork.features.client.customer.dto.response.CustomerSummaryresponse;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.mapper.interfaces.CustomerMapper;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Transactional
@Slf4j
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepo;
    private final CustomerMapper customerMapper;
    private final AddressService addressService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final KycAutomationService kycAutomationService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Customer createCustomer(CustomerRequest request) {

        var validationErrors = validateCustomer(request);

        if(!validationErrors.isEmpty()){
            log.debug("Invalid customer request");
            throw new IllegalArgumentException("Invalid customer request");
        }

        Customer obj = customerMapper.toEntity(request);
        if (request.getAddress() != null) {
            Address persistedAddress = addressService.createAddress(request.getAddress());
            obj.setAddress(persistedAddress);
        }
        String customerTypeAbbrev = obj.getType() != null ? CodeComposer.abbrev(obj.getType().name()) : "CLT";
        long customerSeq = CodeComposer.extractSeq(sequenceGenerator.next("customer", LocalDate.now()));
        obj.setCustomerId(CodeComposer.withMonth("CUS", customerTypeAbbrev, LocalDate.now(), customerSeq));
        obj.setStatus(CustomerStatus.ACTIVE);
        customerRepo.save(obj);
        kycAutomationService.initializeCustomerKyc(obj.getCustomerId());
        return obj;
    }


    @Override
    public void updateCustomer(String customerId, CustomerRequest request) {

        var validationErrors = validateCustomer(request);

        if(!validationErrors.isEmpty()){
            log.debug("Invalid customer request");
            throw new IllegalArgumentException("Invalid customer request");
        }

        Customer obj = customerRepo.findByCustomerId(customerId)
                .orElseThrow(()-> new IllegalArgumentException("Customer with id " + customerId + " not found"));

        customerRepo.save(customerMapper.updateEntity(obj, request));
    }

    @Override
    public void changeStatus(String customerId, ChangeCustomerStatusRequest request) {
        Customer customer = customerRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found"));
        customer.setStatus(request.getStatus());
        customerRepo.save(customer);
        kycAutomationService.syncCustomerKyc(customer.getCustomerId());
    }

    @Override
    public CustomerResponse getCustomerByCustomerId(String code) {

        return customerMapper.toDto(customerRepo.findByCustomerId(code)
                .orElseThrow(()-> new IllegalArgumentException("Customer with id " + code + " not found")));

    }

    @Override
    public  CustomerResponse getCustomerByEmail(String email) {

        log.debug("Getting customer by email {}", email);

        if(email.isEmpty() || !ValidationUtils.validateEmail(email)){
            throw new IllegalArgumentException("Invalid customer email");
        }

        return customerMapper.toDto(customerRepo.findByEmail(email)
                .orElseThrow(()-> new IllegalArgumentException("Customer with email " + email + " not found")));
    }

    @Override
    public void deleteCustomer(String customerId) {

        if(!customerRepo.existsByCustomerId(customerId)){
            throw new ResourceNotFoundException("Customer with id " + customerId + " not found");
        }

        customerRepo.deleteByCustomerId(customerId);
    }
    @Transactional(readOnly = true)
    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepo.findAll().stream()
                .map(customerMapper::toDto)
                .toList();
    }

    @Override
    public Customer getCustomerForService(Long id){

        if(!customerRepo.existsById(id)){
            throw new ResourceNotFoundException("Customer with id " + id + " not found");
        }

        log.debug("Getting customer by id {}", id);
        return customerRepo.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Customer with id " + id + " not found"));
    }

    @Override
    public Customer getCustomerForService(String customerId){
        String normalizedCustomerId = customerId == null ? null : customerId.trim();
        if (!StringUtils.hasText(normalizedCustomerId)) {
            throw new ResourceNotFoundException("Customer with id " + customerId + " not found");
        }

        log.debug("Getting customer by id {}", normalizedCustomerId);
        return customerRepo.findByCustomerId(normalizedCustomerId)
                .or(() -> findCustomerByNumericId(normalizedCustomerId))
                .orElseThrow(()-> new ResourceNotFoundException("Customer with id " + customerId + " not found"));
    }

    @Override
    public List<CustomerSummaryresponse> getAllCustomersSummary(){

        return customerRepo.findAll().stream()
                .map(customerMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerSummaryresponse> basicSearch(String query, CustomerType type) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("Search query is required");
        }

        return customerRepo.basicSearch(query.trim(), type).stream()
                .map(customerMapper::toSummary)
                .toList();
    }

    @Override
    public PaginatedResponse<CustomerResponse> list(Pageable page){

        Page<CustomerResponse> customers = customerRepo.findAll(page).map(customerMapper::toDto);
        return new PaginatedResponse<>(customers);

    }

    @Override
    public PaginatedResponse<CustomerSummaryresponse> listSummary(Pageable page){

        Page<CustomerSummaryresponse> customers = customerRepo.findAll(page).map(customerMapper::toSummary);
        return new PaginatedResponse<>(customers);

    }

    private List<String> validateCustomer(CustomerRequest request) {

        var errors = new ArrayList<String>();

        if (request == null) {
            errors.add("Invalid customer request");
            return errors;
        }

        CustomerType customerType = parseCustomerType(request.getType());

        if (customerType == null) {
            errors.add("Invalid customer request, the type is not valid");
            return errors;
        }

        if (customerType == CustomerType.PERSON) {
            if (!StringUtils.hasText(request.getFirstname()) || !ValidationUtils.validateString(request.getFirstname())) {
                errors.add("Invalid customer request, the firstname is not valid");
            }
            if (!StringUtils.hasText(request.getLastname()) || !ValidationUtils.validateString(request.getLastname())) {
                errors.add("Invalid customer request, the lastname is not valid");
            }
        }

        if (customerType == CustomerType.COMPANY
                && !StringUtils.hasText(request.getCompanyName())) {
            errors.add("Invalid customer request, the company name is required");
        }

        if (!StringUtils.hasText(request.getEmail()) || !ValidationUtils.validateEmail(request.getEmail())) {
            errors.add("Invalid customer request, the email is not valid");
        }

        if (StringUtils.hasText(request.getBillingEmail()) && !ValidationUtils.validateEmail(request.getBillingEmail())) {
            errors.add("Invalid customer request, the billing email is not valid");
        }

        if (!StringUtils.hasText(request.getPhone()) || ValidationUtils.validatePhoneNumber(request.getPhone())) {
            errors.add("Invalid customer request, the phone number is not valid");
        }

//        if (request.getWhatsappPhone().isEmpty() || !ValidationUtils.validatePhoneNumber(request.getWhatsappPhone())) {
//            errors.add("Invalid customer request, the whatsapp phone number is not valid");
//
//
//        }

        return errors;
    }

    private CustomerType parseCustomerType(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return CustomerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Optional<Customer> findCustomerByNumericId(String value) {
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        try {
            return customerRepo.findById(Long.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

}

