package com.sni.bokaticowork.features.client.customer.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
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
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.utils.code.CodeComposer;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycAutomationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

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
    private final MemberRepository memberRepository;
    private final CustomerMapper customerMapper;
    private final AddressService addressService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final KycAutomationService kycAutomationService;
    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine emailTemplateEngine;
    private final com.sni.bokaticowork.core.utils.phone.PhoneNumberService phoneNumberService;

    @Value("${app.verify-base-url:}")
    private String publicBaseUrl;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Customer createCustomer(CustomerRequest request) {
        return createCustomer(request, true, true);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Customer createCustomerForMember(CustomerRequest request) {
        return createCustomer(request, false, false);
    }

    private Customer createCustomer(CustomerRequest request, boolean initializeKyc, boolean sendWelcomeEmail) {

        var validationErrors = validateCustomer(request);

        if(!validationErrors.isEmpty()){
            // Le detail est renvoye : un refus qui ne dit pas quel champ est en cause oblige a
            // deviner, et ce que l'on devine mal, on le renvoie tel quel.
            throw new BadRequestException(String.join(", ", validationErrors));
        }
        normalizePhones(request);

        if (StringUtils.hasText(request.getEmail())
                && Boolean.TRUE.equals(customerRepo.existsByEmailAndDeletedFalse(request.getEmail().trim().toLowerCase(Locale.ROOT)))) {
            throw new ResourceAlreadyExistException("A customer with this email already exists");
        }
        if (StringUtils.hasText(request.getPhone())) {
            Optional<Customer> byPhone = customerRepo.findByPhoneAndDeletedFalse(request.getPhone());
            if (byPhone.isPresent()) {
                throw new ResourceAlreadyExistException("A customer with this phone number already exists");
            }
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
        if (initializeKyc) {
            kycAutomationService.initializeCustomerKyc(obj.getCustomerId());
        }
        if (sendWelcomeEmail) {
            sendCustomerCreatedEmailAfterCommit(obj);
        }
        return obj;
    }


    @Override
    public void updateCustomer(String customerId, CustomerRequest request) {

        var validationErrors = validateCustomer(request);

        if(!validationErrors.isEmpty()){
            throw new BadRequestException(String.join(", ", validationErrors));
        }
        normalizePhones(request);

        Customer obj = customerRepo.findByCustomerId(customerId)
                .orElseThrow(()-> new ResourceNotFoundException("Customer with id " + customerId + " not found"));

        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().trim().equalsIgnoreCase(obj.getEmail())
                && Boolean.TRUE.equals(customerRepo.existsByEmailAndIdNot(request.getEmail().trim().toLowerCase(Locale.ROOT), obj.getId()))) {
            throw new ResourceAlreadyExistException("A customer with this email already exists");
        }
        if (StringUtils.hasText(request.getPhone())
                && !request.getPhone().replaceAll("\\s+", "").equals(obj.getPhone())
                && Boolean.TRUE.equals(customerRepo.existsByPhoneAndIdNot(request.getPhone().replaceAll("\\s+", ""), obj.getId()))) {
            throw new ResourceAlreadyExistException("A customer with this phone number already exists");
        }

        customerRepo.save(customerMapper.updateEntity(obj, request));
    }

    private static final java.util.Map<CustomerStatus, java.util.Set<CustomerStatus>> ALLOWED_STATUS_TRANSITIONS = java.util.Map.of(
            CustomerStatus.PENDING, java.util.Set.of(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE, CustomerStatus.ARCHIVED),
            CustomerStatus.ACTIVE, java.util.Set.of(CustomerStatus.SUSPENDED, CustomerStatus.INACTIVE, CustomerStatus.ARCHIVED),
            CustomerStatus.SUSPENDED, java.util.Set.of(CustomerStatus.ACTIVE, CustomerStatus.INACTIVE, CustomerStatus.ARCHIVED),
            CustomerStatus.INACTIVE, java.util.Set.of(CustomerStatus.ACTIVE, CustomerStatus.ARCHIVED),
            CustomerStatus.ARCHIVED, java.util.Set.of()
    );

    @Override
    public void changeStatus(String customerId, ChangeCustomerStatusRequest request) {
        Customer customer = customerRepo.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found"));
        CustomerStatus current = customer.getStatus();
        CustomerStatus target = request.getStatus();
        java.util.Set<CustomerStatus> allowed = ALLOWED_STATUS_TRANSITIONS.getOrDefault(current, java.util.Set.of());
        if (!allowed.contains(target)) {
            throw new BadRequestException("Status transition from " + current + " to " + target + " is not allowed");
        }
        customer.setStatus(target);
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
        if (!customerRepo.existsByCustomerIdAndDeletedFalse(customerId)) {
            throw new ResourceNotFoundException("Customer with id " + customerId + " not found");
        }
        memberRepository.suppressAllByCustomerId(customerId);
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
        Customer customer = customerRepo.findByCustomerId(normalizedCustomerId)
                .or(() -> findCustomerByNumericId(normalizedCustomerId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer with id " + customerId + " not found"));
        if (customer.isDeleted()) {
            throw new BadRequestException("Le client " + customerId + " est supprimé et ne peut plus être utilisé pour de nouvelles opérations");
        }
        return customer;
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

    /** Forme internationale, avec le pays fourni ou celui de l'etablissement si l'indicatif manque. */
    private void normalizePhones(CustomerRequest request) {
        request.setPhone(phoneNumberService.normalize(request.getPhone(), request.getPhoneCountry()));
        request.setWhatsappPhone(phoneNumberService.normalizeOptional(request.getWhatsappPhone(), request.getPhoneCountry()));
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

        if (!StringUtils.hasText(request.getPhone()) || !ValidationUtils.validatePhoneNumber(request.getPhone())) {
            errors.add("Invalid customer request, the phone number is not valid");
        }

        if (StringUtils.hasText(request.getWhatsappPhone()) && !ValidationUtils.validatePhoneNumber(request.getWhatsappPhone())) {
            errors.add("Invalid customer request, the whatsapp phone number is not valid");
        }

        return errors;
    }

    private void sendCustomerCreatedEmailAfterCommit(Customer customer) {
        if (customer == null || !StringUtils.hasText(customer.getEmail())) {
            return;
        }
        Runnable task = () -> {
            try {
                Context context = new Context();
                context.setVariable("name", customerDisplayName(customer));
                context.setVariable("customerId", customer.getCustomerId());
                context.setVariable("customerType", customer.getType() == null ? null : customer.getType().name());
                context.setVariable("email", customer.getEmail());
                context.setVariable("logoUrl", logoUrl());
                String html = emailTemplateEngine.process("customer-created", context);
                emailSender.sendHtmlEmail(customer.getEmail(), "Bienvenue chez Bokati", html);
            } catch (MessagingException ex) {
                log.warn("Unable to send customer creation email to {}", customer.getEmail(), ex);
            }
        };
        runAfterCommit(task);
    }

    private String customerDisplayName(Customer customer) {
        if (customer.getType() == CustomerType.COMPANY && StringUtils.hasText(customer.getCompanyName())) {
            return customer.getCompanyName();
        }
        String name = ((customer.getFirstname() == null ? "" : customer.getFirstname()) + " "
                + (customer.getLastname() == null ? "" : customer.getLastname())).trim();
        return StringUtils.hasText(name) ? name : customer.getEmail();
    }

    private String logoUrl() {
        if (!StringUtils.hasText(publicBaseUrl)) {
            return "/images/logo.png";
        }
        return publicBaseUrl.replaceAll("/+$", "") + "/images/logo.png";
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
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
