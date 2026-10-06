package com.sni.bokaticowork.features.company.service.implementation;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.model.Currency;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CurrencyService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.features.company.dto.request.BusinessEntityRequest;
import com.sni.bokaticowork.features.company.dto.request.BusinessSearchCriteria;
import com.sni.bokaticowork.features.company.dto.request.UpdateBusinessStatusRequest;
import com.sni.bokaticowork.features.company.dto.response.BusinessEntityResponse;
import com.sni.bokaticowork.features.company.enums.LegalForm;
import com.sni.bokaticowork.features.company.enums.Status;
import com.sni.bokaticowork.features.company.mapper.interfaces.BusinessMapper;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.company.repository.specification.BusinessSpecification;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class BusinessServiceImpl implements BusinessService {

    private final BusinessMapper businessMapper;
    private final BusinessRepository businessRepo;
    private final AddressService addressService;
    private final CurrencyService currencyService;

    @Override
    public BusinessEntityResponse createBusiness(BusinessEntityRequest request) {
        List<String> errors = validateBusiness(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid business request", errors);
        }

        ensureBusinessDoesNotExist(request);

        Currency currency = resolveCurrency(request.getBaseCurrencyCode());

        BusinessEntity business = businessMapper.toEntity(request);
        Address address = request.getAddress() == null ? null : addressService.createAddress(request.getAddress());

        business.setBaseCurrency(currency);
        business.setAddress(address);
        business.setCode(generateBusinessCode(business.getName()));
        business.setStatus(Status.ACTIVE);
        business.setCreated_by("SYSTEM");

        businessRepo.save(business);
        return businessMapper.toDTO(business);
    }

    @Override
    public BusinessEntityResponse updateBusiness(String businessCode, BusinessEntityRequest request) {
        List<String> errors = validateBusiness(request);
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid business request", errors);
        }

        BusinessEntity business = getBusinessForService(businessCode);

        if (!business.getName().equalsIgnoreCase(request.getName())
                && businessRepo.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ResourceAlreadyExistException("A business with this name already exists");
        }

        // Les deux cotes peuvent etre absents · la comparaison le faisait tomber en NPE des que
        // le NIU est devenu facultatif.
        String requestedNiu = StringUtils.hasText(request.getNiuNumber())
                ? request.getNiuNumber().trim() : null;
        if (requestedNiu != null
                && !requestedNiu.equalsIgnoreCase(business.getNiuNumber())
                && businessRepo.existsByNiuNumber(requestedNiu.toUpperCase(Locale.ROOT))) {
            throw new ResourceAlreadyExistException("A business with this NIU number already exists");
        }

        if (!business.getRccmNumber().equalsIgnoreCase(request.getRccmNumber().trim())
                && businessRepo.existsByRccmNumber(request.getRccmNumber().trim().toUpperCase(Locale.ROOT))) {
            throw new ResourceAlreadyExistException("A business with this RCCM number already exists");
        }

        businessMapper.mapUpdateRequestToBusiness(business, request);
        business.setBaseCurrency(resolveCurrency(request.getBaseCurrencyCode()));
        business.setUpdated_by("SYSTEM");

        if (request.getAddress() != null) {
            if (business.getAddress() == null) {
                business.setAddress(addressService.createAddress(request.getAddress()));
            } else {
                business.setAddress(addressService.updateAddress(business.getAddress().getId(), request.getAddress()));
            }
        }

        businessRepo.save(business);
        return businessMapper.toDTO(business);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessEntityResponse getBusinessByCode(String code) {
        return businessMapper.toDTO(getBusinessForService(code));
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessEntityResponse getBusinessByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BadRequestException("Business name is required");
        }

        BusinessEntity business = businessRepo.findByNameIgnoreCase(name.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Business with name " + name.trim() + " not found"));

        return businessMapper.toDTO(business);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessEntityResponse> basicSearch(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Search query is required");
        }

        return businessRepo.basicSearch(query.trim()).stream()
                .map(businessMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessEntityResponse> getAllBusiness() {
        return businessRepo.findAll().stream()
                .map(businessMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BusinessEntityResponse> list(Pageable pageable) {
        Page<BusinessEntityResponse> page = businessRepo.findAll(pageable).map(businessMapper::toDTO);
        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<BusinessEntityResponse> search(BusinessSearchCriteria criteria, Pageable pageable) {
        validateSearchCriteria(criteria);
        Specification<BusinessEntity> specification = BusinessSpecification.search(criteria);
        Page<BusinessEntityResponse> page = businessRepo.findAll(specification, pageable).map(businessMapper::toDTO);
        return new PaginatedResponse<>(page);
    }

    @Override
    public void updateBusinessAddress(String businessCode, AddressRequest request) {
        if (request == null) {
            throw new BadRequestException("Address request is required");
        }

        BusinessEntity business = getBusinessForService(businessCode);

        if (business.getAddress() == null) {
            business.setAddress(addressService.createAddress(request));
        } else {
            business.setAddress(addressService.updateAddress(business.getAddress().getId(), request));
        }

        business.setUpdated_by("SYSTEM");
        businessRepo.save(business);
    }

    @Override
    public void updateBusinessStatus(String businessCode, UpdateBusinessStatusRequest request) {
        if (request == null || !StringUtils.hasText(request.getStatus())) {
            throw new BadRequestException("Business status is required");
        }

        BusinessEntity business = getBusinessForService(businessCode);
        try {
            business.setStatus(Status.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid business status: " + request.getStatus(), ex);
        }

        business.setUpdated_by("SYSTEM");
        businessRepo.save(business);
    }

    /**
     * Designe l entite exploitante · le drapeau est retire a celle qui le portait.
     *
     * <p>Le retrait precede la pose, et les deux sont dans la meme transaction · l index unique
     * partiel refuserait deux porteurs, et laisser la base dans un etat sans exploitant, meme un
     * instant, suffirait a faire echouer une generation de contrat concurrente.</p>
     */
    @Override
    public void designateOperatingBusiness(String businessCode) {
        BusinessEntity business = getBusinessForService(businessCode);
        if (business.isOperator()) {
            return;
        }
        businessRepo.findFirstByOperatorTrueAndDeletedFalse().ifPresent(previous -> {
            previous.setOperator(false);
            previous.setUpdated_by("SYSTEM");
            businessRepo.saveAndFlush(previous);
        });
        business.setOperator(true);
        business.setUpdated_by("SYSTEM");
        businessRepo.save(business);
        log.info("Entite exploitante designee · {} ({})", business.getName(), business.getCode());
    }

    @Override
    public void deleteBusiness(String businessCode) {
        BusinessEntity business = getBusinessForService(businessCode);
        businessRepo.softDelete(business.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessEntity serviceBusinessByCode(String code) {
        return getBusinessForService(code);
    }

    @Override
    @Transactional(readOnly = true)
    public BusinessEntity serviceBusinessById(Long id) {
        return businessRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business with id " + id + " not found"));
    }

    private BusinessEntity getBusinessForService(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Business code is required");
        }

        return businessRepo.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Business with code " + code.trim() + " not found"));
    }

    private void ensureBusinessDoesNotExist(BusinessEntityRequest request) {
        if (businessRepo.existsByNameIgnoreCase(request.getName().trim())) {
            throw new ResourceAlreadyExistException("A business with this name already exists");
        }

        if (StringUtils.hasText(request.getNiuNumber())
                && businessRepo.existsByNiuNumber(request.getNiuNumber().trim().toUpperCase(Locale.ROOT))) {
            throw new ResourceAlreadyExistException("A business with this NIU number already exists");
        }

        if (businessRepo.existsByRccmNumber(request.getRccmNumber().trim().toUpperCase(Locale.ROOT))) {
            throw new ResourceAlreadyExistException("A business with this RCCM number already exists");
        }
    }

    private Currency resolveCurrency(String currencyCode) {
        try {
            return currencyService.serviceCurrencyByCode(currencyCode.trim().toUpperCase(Locale.ROOT));
        } catch (ResourceNotFoundException ex) {
            throw new BadRequestException("Currency with code " + currencyCode + " not found", ex);
        }
    }

    private List<String> validateBusiness(BusinessEntityRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Invalid business request, the request body is required");
            return errors;
        }

        if (!StringUtils.hasText(request.getName()) || !ValidationUtils.validateString(request.getName().trim())) {
            errors.add("Invalid business request, the name is not valid");
        }

        if (!isValidLegalForm(request.getLegalForm())) {
            errors.add("Invalid business request, the legal form is not valid");
        }

        // Le NIU est facultatif · verifie seulement s il est fourni. L exiger empechait
        // d enregistrer une entite qui n en a pas encore, l espace lui-meme compris.
        if (StringUtils.hasText(request.getNiuNumber())
                && !ValidationUtils.validateNiu(request.getNiuNumber().trim().toUpperCase(Locale.ROOT))) {
            errors.add("Invalid business request, the NIU number is not valid");
        }

        if (!isValidBusinessIdentifier(request.getRccmNumber())) {
            errors.add("Invalid business request, the RCCM number is not valid");
        }

        if (StringUtils.hasText(request.getTaxId()) && !isValidBusinessIdentifier(request.getTaxId())) {
            errors.add("Invalid business request, the tax id is not valid");
        }

        if (StringUtils.hasText(request.getActivity()) && !isValidBusinessText(request.getActivity())) {
            errors.add("Invalid business request, the activity is not valid");
        }

        if (!StringUtils.hasText(request.getPhone()) || !ValidationUtils.validatePhoneNumber(request.getPhone().replaceAll("\\s+", ""))) {
            errors.add("Invalid business request, the phone number is not valid");
        }

        if (!StringUtils.hasText(request.getEmail()) || !ValidationUtils.validateEmail(request.getEmail().trim().toLowerCase(Locale.ROOT))) {
            errors.add("Invalid business request, the email is not valid");
        }

        if (!StringUtils.hasText(request.getBaseCurrencyCode()) || request.getBaseCurrencyCode().trim().length() != 3) {
            errors.add("Invalid business request, the base currency code is not valid");
        }

        return errors;
    }

    private boolean isValidLegalForm(String legalForm) {
        if (!StringUtils.hasText(legalForm)) {
            return false;
        }

        try {
            LegalForm.valueOf(legalForm.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean isValidBusinessIdentifier(String value) {
        return StringUtils.hasText(value) && value.trim().matches("^[A-Za-z0-9/_ -]+$");
    }

    private boolean isValidBusinessText(String value) {
        return StringUtils.hasText(value) && value.trim().matches("^[A-Za-z0-9 ,/_-]+$");
    }

    private String generateBusinessCode(String name) {
        String prefix = name.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (prefix.length() < 3) {
            prefix = (prefix + "BIZ").substring(0, 3);
        } else {
            prefix = prefix.substring(0, 3);
        }

        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String code;

        do {
            int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
            code = prefix + date + suffix;
        } while (businessRepo.existsByCode(code));

        return code;
    }

    private void validateSearchCriteria(BusinessSearchCriteria criteria) {
        if (criteria == null || isSearchCriteriaEmpty(criteria)) {
            return;
        }

        if (StringUtils.hasText(criteria.getStatus())) {
            try {
                Status.valueOf(criteria.getStatus().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid business status: " + criteria.getStatus(), ex);
            }
        }

        if (StringUtils.hasText(criteria.getLegalForm()) && !isValidLegalForm(criteria.getLegalForm())) {
            throw new BadRequestException("Invalid business legal form: " + criteria.getLegalForm());
        }

        if (StringUtils.hasText(criteria.getEmail()) && !ValidationUtils.validateEmail(criteria.getEmail().trim().toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Invalid business email");
        }
    }

    private boolean isSearchCriteriaEmpty(BusinessSearchCriteria criteria) {
        return !StringUtils.hasText(criteria.getCode())
                && !StringUtils.hasText(criteria.getName())
                && !StringUtils.hasText(criteria.getLegalForm())
                && !StringUtils.hasText(criteria.getNiuNumber())
                && !StringUtils.hasText(criteria.getRccmNumber())
                && !StringUtils.hasText(criteria.getTaxId())
                && !StringUtils.hasText(criteria.getActivity())
                && !StringUtils.hasText(criteria.getPhone())
                && !StringUtils.hasText(criteria.getEmail())
                && !StringUtils.hasText(criteria.getBaseCurrencyCode())
                && !StringUtils.hasText(criteria.getStatus());
    }
}
