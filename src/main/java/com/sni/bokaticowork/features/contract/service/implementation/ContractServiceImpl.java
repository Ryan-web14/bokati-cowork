package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import com.sni.bokaticowork.features.contract.dto.request.ContractPartyRequest;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.UpdateContractStatusRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractPartyResponse;
import com.sni.bokaticowork.features.contract.dto.response.ContractResponse;
import com.sni.bokaticowork.features.contract.enums.ContractPartyRole;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.mapper.interfaces.ContractMapper;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractParty;
import com.sni.bokaticowork.features.contract.repository.ContractPartyRepository;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractPartyRepository contractPartyRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final MemberService memberService;
    private final CustomerService customerService;
    private final BusinessService businessService;
    private final ContractMapper mapper;

    @Override
    public ContractResponse create(CreateContractRequest request) {
        validateDates(request.getEffectiveDate(), request.getStartDate(), request.getEndDate());
        validateParties(request.getParties());
        OwnerResolution owner = resolveOwner(request.getOwnerType(), request.getOwnerCode());
        BusinessEntity business = resolveBusiness(request.getBusinessCode());

        Contract contract = Contract.builder()
                .contractCode(sequenceGenerator.next("CONTRACT", LocalDate.now()))
                .title(request.getTitle().trim())
                .description(trimToNull(request.getDescription()))
                .templateCode(request.getTemplateCode().trim().toLowerCase(Locale.ROOT))
                .ownerType(request.getOwnerType())
                .ownerId(owner.id())
                .ownerCode(owner.code())
                .business(business)
                .renewalType(request.getRenewalType())
                .effectiveDate(request.getEffectiveDate())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(request.getCreatedBy())
                .status(ContractStatus.DRAFT)
                .build();
        contractRepository.save(contract);
        replaceParties(contract, request.getParties());
        return toResponse(contract);
    }

    @Override
    public ContractResponse update(String contractCode, UpdateContractRequest request) {
        Contract contract = serviceByCode(contractCode);
        ensureMutable(contract);
        if (StringUtils.hasText(request.getTitle())) {
            contract.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            contract.setDescription(trimToNull(request.getDescription()));
        }
        if (StringUtils.hasText(request.getTemplateCode())) {
            contract.setTemplateCode(request.getTemplateCode().trim().toLowerCase(Locale.ROOT));
        }
        if (request.getBusinessCode() != null) {
            contract.setBusiness(resolveBusiness(request.getBusinessCode()));
        }
        if (request.getRenewalType() != null) {
            contract.setRenewalType(request.getRenewalType());
        }
        if (request.getEffectiveDate() != null) {
            contract.setEffectiveDate(request.getEffectiveDate());
        }
        if (request.getStartDate() != null) {
            contract.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            contract.setEndDate(request.getEndDate());
        }
        validateDates(contract.getEffectiveDate(), contract.getStartDate(), contract.getEndDate());
        contractRepository.save(contract);
        if (request.getParties() != null) {
            validateParties(request.getParties());
            replaceParties(contract, request.getParties());
        }
        return toResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getByCode(String contractCode) {
        return toResponse(serviceByCode(contractCode));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ContractResponse> list(DocumentOwnerType ownerType, String ownerCode, ContractStatus status, String businessCode, String templateCode, Pageable pageable) {
        Specification<Contract> spec = (root, query, cb) -> cb.conjunction();
        if (ownerType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerType"), ownerType));
        }
        if (StringUtils.hasText(ownerCode)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerCode"), ownerCode.trim()));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(businessCode)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("business").get("code"), businessCode.trim()));
        }
        if (StringUtils.hasText(templateCode)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("templateCode"), templateCode.trim().toLowerCase(Locale.ROOT)));
        }
        Page<Contract> page = contractRepository.findAll(spec, pageable);
        return new PaginatedResponse<>(page.map(this::toResponse));
    }

    @Override
    public ContractResponse updateStatus(String contractCode, UpdateContractStatusRequest request) {
        Contract contract = serviceByCode(contractCode);
        ContractStatus status;
        try {
            status = ContractStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Statut de contrat invalide: " + request.getStatus(), ex);
        }
        return applyStatusTransition(contract, status, trimToNull(request.getReason()));
    }

    @Override
    public ContractResponse markGenerated(String contractCode, String documentCode) {
        Contract contract = serviceByCode(contractCode);
        ensureNotDeleted(contract);
        if (!StringUtils.hasText(documentCode)) {
            throw new BadRequestException("Le code du document brouillon est requis");
        }
        validateTransition(contract.getStatus(), ContractStatus.GENERATED);
        contract.setDraftDocumentCode(documentCode);
        contract.setStatus(nextStatusAfterDraftGeneration(contract));
        contractRepository.save(contract);
        return toResponse(contract);
    }

    @Override
    public ContractResponse markSigned(String contractCode, String documentCode) {
        Contract contract = serviceByCode(contractCode);
        ensureNotDeleted(contract);
        if (!StringUtils.hasText(documentCode)) {
            throw new BadRequestException("Le code du document signe est requis");
        }
        validateTransition(contract.getStatus(), ContractStatus.SIGNED);
        contract.setSignedDocumentCode(documentCode);
        contract.setSignedAt(Instant.now());
        contract.setStatus(shouldAutoActivate(contract) ? ContractStatus.ACTIVE : ContractStatus.SIGNED);
        if (contract.getStatus() == ContractStatus.ACTIVE) {
            contract.setActivatedAt(Instant.now());
        }
        contractRepository.save(contract);
        return toResponse(contract);
    }

    @Override
    public ContractResponse approveReview(String contractCode, Long reviewedBy, String comment) {
        Contract contract = serviceByCode(contractCode);
        ensureNotDeleted(contract);
        validateTransition(contract.getStatus(), ContractStatus.AWAITING_SIGNATURE);
        contract.setStatus(ContractStatus.AWAITING_SIGNATURE);
        contract.setReviewedBy(reviewedBy);
        contract.setReviewComment(comment);
        contractRepository.save(contract);
        return toResponse(contract);
    }

    @Override
    public ContractResponse rejectReview(String contractCode, Long reviewedBy, String comment) {
        Contract contract = serviceByCode(contractCode);
        ensureNotDeleted(contract);
        if (contract.getStatus() != ContractStatus.UNDER_REVIEW) {
            throw new BadRequestException("Le contrat doit être en statut UNDER_REVIEW pour être rejeté");
        }
        validateTransition(contract.getStatus(), ContractStatus.GENERATED);
        contract.setStatus(ContractStatus.GENERATED);
        contract.setReviewedBy(reviewedBy);
        contract.setReviewComment(comment);
        contractRepository.save(contract);
        return toResponse(contract);
    }

    @Override
    public ContractResponse activate(String contractCode) {
        return applyStatusTransition(serviceByCode(contractCode), ContractStatus.ACTIVE, null);
    }

    @Override
    public ContractResponse suspend(String contractCode, String reason) {
        return applyStatusTransition(serviceByCode(contractCode), ContractStatus.SUSPENDED, trimToNull(reason));
    }

    @Override
    public ContractResponse terminate(String contractCode, String reason) {
        return applyStatusTransition(serviceByCode(contractCode), ContractStatus.TERMINATED, trimToNull(reason));
    }

    @Override
    public ContractResponse cancel(String contractCode, String reason) {
        return applyStatusTransition(serviceByCode(contractCode), ContractStatus.CANCELLED, trimToNull(reason));
    }

    @Override
    public void delete(String contractCode) {
        Contract contract = serviceByCode(contractCode);
        contractRepository.delete(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public Contract serviceByCode(String contractCode) {
        if (!StringUtils.hasText(contractCode)) {
            throw new BadRequestException("Le code du contrat est requis");
        }
        return contractRepository.findByContractCode(contractCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable"));
    }

    private void replaceParties(Contract contract, List<ContractPartyRequest> parties) {
        contractPartyRepository.deleteAll(contractPartyRepository.findAllByContractOrderBySignOrderAscIdAsc(contract));
        if (parties == null || parties.isEmpty()) {
            return;
        }

        for (ContractPartyRequest request : parties) {
            OwnerResolution party = resolveOwner(request.getPartyType(), request.getPartyCode());
            ContractParty entity = ContractParty.builder()
                    .contract(contract)
                    .partyType(request.getPartyType())
                    .partyId(party.id())
                    .partyCode(party.code())
                    .displayName(request.getDisplayName().trim())
                    .email(trimToNull(request.getEmail()))
                    .phone(trimToNull(request.getPhone()))
                    .role(request.getRole())
                    .signOrder(request.getSignOrder())
                    .mustSign(request.getMustSign() == null ? Boolean.FALSE : request.getMustSign())
                    .build();
            contractPartyRepository.save(entity);
        }
    }

    private ContractResponse applyStatusTransition(Contract contract, ContractStatus targetStatus, String reason) {
        ensureNotDeleted(contract);
        validateTransition(contract.getStatus(), targetStatus);

        contract.setStatus(targetStatus);
        switch (targetStatus) {
            case ACTIVE -> {
                contract.setActivatedAt(Instant.now());
                contract.setSuspensionReason(null);
            }
            case TERMINATED, CANCELLED -> {
                contract.setTerminatedAt(Instant.now());
                contract.setTerminationReason(reason);
            }
            case SUSPENDED -> {
                contract.setSuspensionReason(reason);
            }
            default -> { }
        }

        if (targetStatus != ContractStatus.TERMINATED && targetStatus != ContractStatus.CANCELLED) {
            contract.setTerminatedAt(null);
            contract.setTerminationReason(null);
        }

        contractRepository.save(contract);
        return toResponse(contract);
    }

    private void validateDates(LocalDate effectiveDate, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("La date de fin doit etre posterieure ou egale a la date de debut");
        }
        if (effectiveDate != null && startDate != null && effectiveDate.isAfter(startDate)) {
            throw new BadRequestException("La date d'effet ne peut pas etre apres la date de debut");
        }
    }

    private void validateParties(List<ContractPartyRequest> parties) {
        if (parties == null || parties.isEmpty()) {
            return;
        }
        long signatories = parties.stream()
                .filter(item -> item.getRole() == ContractPartyRole.SIGNATORY || Boolean.TRUE.equals(item.getMustSign()))
                .count();
        if (signatories == 0) {
            throw new BadRequestException("Le contrat doit contenir au moins un signataire");
        }
        for (ContractPartyRequest party : parties) {
            if (!StringUtils.hasText(party.getDisplayName())) {
                throw new BadRequestException("Le nom d'affichage d'une partie est requis");
            }
        }
    }

    private void ensureMutable(Contract contract) {
        if (contract.getStatus() == ContractStatus.TERMINATED
                || contract.getStatus() == ContractStatus.CANCELLED
                || contract.getStatus() == ContractStatus.EXPIRED) {
            throw new BadRequestException("Ce contrat n'est plus modifiable dans son statut actuel");
        }
    }

    private void ensureNotDeleted(Contract contract) {
        if (Boolean.TRUE.equals(contract.getDeleted())) {
            throw new ResourceNotFoundException("Contrat introuvable");
        }
    }

    private void validateTransition(ContractStatus currentStatus, ContractStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }
        boolean allowed = switch (currentStatus) {
            case DRAFT -> targetStatus == ContractStatus.GENERATED
                    || targetStatus == ContractStatus.UNDER_REVIEW
                    || targetStatus == ContractStatus.CANCELLED;
            case GENERATED -> targetStatus == ContractStatus.UNDER_REVIEW
                    || targetStatus == ContractStatus.AWAITING_SIGNATURE
                    || targetStatus == ContractStatus.CANCELLED;
            case UNDER_REVIEW -> targetStatus == ContractStatus.AWAITING_SIGNATURE
                    || targetStatus == ContractStatus.GENERATED
                    || targetStatus == ContractStatus.CANCELLED;
            case AWAITING_SIGNATURE -> targetStatus == ContractStatus.SIGNED
                    || targetStatus == ContractStatus.GENERATED
                    || targetStatus == ContractStatus.CANCELLED;
            case SIGNED -> targetStatus == ContractStatus.ACTIVE
                    || targetStatus == ContractStatus.CANCELLED;
            case ACTIVE -> targetStatus == ContractStatus.SUSPENDED
                    || targetStatus == ContractStatus.TERMINATED
                    || targetStatus == ContractStatus.EXPIRED;
            case SUSPENDED -> targetStatus == ContractStatus.ACTIVE
                    || targetStatus == ContractStatus.TERMINATED
                    || targetStatus == ContractStatus.CANCELLED;
            case EXPIRED, TERMINATED, CANCELLED -> false;
        };
        if (!allowed) {
            throw new BadRequestException("Transition de statut invalide: " + currentStatus + " -> " + targetStatus);
        }
    }

    private ContractStatus nextStatusAfterDraftGeneration(Contract contract) {
        boolean requiresSignature = contractPartyRepository.findAllByContractOrderBySignOrderAscIdAsc(contract).stream()
                .anyMatch(party -> party.getRole() == ContractPartyRole.SIGNATORY || Boolean.TRUE.equals(party.getMustSign()));
        return requiresSignature ? ContractStatus.AWAITING_SIGNATURE : ContractStatus.UNDER_REVIEW;
    }

    private boolean shouldAutoActivate(Contract contract) {
        LocalDate activationDate = contract.getEffectiveDate() != null ? contract.getEffectiveDate() : contract.getStartDate();
        return activationDate == null || !activationDate.isAfter(LocalDate.now());
    }

    private ContractResponse toResponse(Contract contract) {
        List<ContractPartyResponse> parties = contractPartyRepository.findAllByContractOrderBySignOrderAscIdAsc(contract).stream()
                .map(mapper::toPartyResponse)
                .toList();

        ContractResponse response = mapper.toResponse(contract);
        response.setParties(parties);
        return response;
    }

    private OwnerResolution resolveOwner(DocumentOwnerType ownerType, String ownerCode) {
        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            throw new BadRequestException("Le proprietaire du contrat est requis");
        }
        return switch (ownerType) {
            case MEMBER -> {
                Member member = memberService.getByMemberIdForService(ownerCode.trim());
                yield new OwnerResolution(member.getId(), member.getMemberId());
            }
            case CUSTOMER -> {
                Customer customer = customerService.getCustomerForService(ownerCode.trim());
                yield new OwnerResolution(customer.getId(), customer.getCustomerId());
            }
            case BUSINESS -> {
                BusinessEntity business = businessService.serviceBusinessByCode(ownerCode.trim());
                yield new OwnerResolution(business.getId(), business.getCode());
            }
            default -> throw new BadRequestException("Type de proprietaire non supporte pour un contrat: " + ownerType);
        };
    }

    private BusinessEntity resolveBusiness(String businessCode) {
        if (!StringUtils.hasText(businessCode)) {
            return null;
        }
        return businessService.serviceBusinessByCode(businessCode.trim());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record OwnerResolution(Long id, String code) {
    }
}
