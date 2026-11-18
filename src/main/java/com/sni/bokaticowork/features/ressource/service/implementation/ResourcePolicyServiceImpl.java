package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.retry.policy.RetryPolicy;
import com.sni.bokaticowork.core.retry.service.interfaces.RetryExecutor;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.validation.ValidationUtils;
import com.sni.bokaticowork.features.ressource.dto.request.CreateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.request.UpdateResourcePolicyRequest;
import com.sni.bokaticowork.features.ressource.dto.response.ResourcePolicyResponse;
import com.sni.bokaticowork.features.ressource.mapper.interfaces.ResourcePolicyMapper;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePolicyRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceRepository;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourcePolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ResourcePolicyServiceImpl implements ResourcePolicyService {

    private final ResourcePolicyRepository policyRepository;
    private final ResourcePolicyMapper policyMapper;
    private final ResourceRepository resourceRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final RetryExecutor retryExecutor;
    private final RetryPolicy defaultRetryPolicy;

    @Override
    public com.sni.bokaticowork.features.ressource.dto.response.ResourcePolicyResponse createPolicy(CreateResourcePolicyRequest request) {
        validateForCreate(request);

        String normalizedName = request.getName().trim();

        List<String> errors = validateForCreate(request);

        if(!errors.isEmpty()){
            throw new ValidationException("Demande de politique de ressource invalide", errors);
        }

        if (policyRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistException("Une politique porte déjà ce nom");
        }

        request.setName(normalizedName);
        request.setDescription(normalizeText(request.getDescription()));

        ResourcePolicy policy = policyMapper.toEntity(request);
        policy.setCode(sequenceGenerator.next("resource_policy", LocalDate.now()));

        return policyMapper.toResponse(policyRepository.save(policy));
    }

    @Override
    public void updatePolicy(String code, UpdateResourcePolicyRequest request) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code de la politique est requis");
        }

        ResourcePolicy policy = getPolicyForService(code.trim());
        List<String> errors = validateForUpdate(request, policy);

        if(!errors.isEmpty()){
            throw new ValidationException("Demande de politique de ressource invalide",errors);
        }

        String normalizedName = request.getName() == null ? policy.getName() : request.getName().trim();

        if (!policy.getName().equalsIgnoreCase(normalizedName)
                && policyRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResourceAlreadyExistException("Une politique porte déjà ce nom");
        }

        if (request.getName() != null) {
            request.setName(normalizedName);
        }
        request.setDescription(normalizeText(request.getDescription()));

        policyMapper.updateEntity(policy, request);
        policyRepository.save(policy);
    }

    @Override
    public void updatePolicyStatus(String code, Boolean status) {
        ResourcePolicy policy = getPolicyForService(code);
        policy.setActive(status);
        policyRepository.save(policy);
    }

    @Override
    public void updateCancellationPolicy(String code, Boolean status) {
        ResourcePolicy policy = getPolicyForService(code);
        policy.setAllowCancellation(status);
        policyRepository.save(policy);
    }

    @Override
    public void deletePolicy(String code) {
        ResourcePolicy policy = getPolicyForService(code);

        // Lot R2 : ressources archivees comprises (elles retiennent la cle etrangere ; avant : 500).
        long attached = resourceRepository.countAttachedToPolicy(policy.getId());
        if (attached > 0) {
            throw new ConflictException("La politique " + policy.getCode() + " a encore " + attached
                    + " ressource(s) rattachée(s), archivées comprises : changez-leur de politique d'abord, ou désactivez-la");
        }

        policyRepository.delete(policy);
    }

    @Override
    public void deleteAllPolicies() {
        List<ResourcePolicy> policies = policyRepository.findAll();

        if (policies.isEmpty()) {
            return;
        }

        boolean hasAssignedPolicies = policies.stream()
                .anyMatch(policy -> !resourceRepository.findAllByResourcePolicy(policy).isEmpty());

        if (hasAssignedPolicies) {
            throw new ConflictException("Certaines politiques sont attribuées à des ressources");
        }

        policyRepository.deleteAll(policies);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourcePolicyResponse getPolicy(String code) {
        return policyMapper.toResponse(getPolicyForService(code));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourcePolicy getPolicyForService(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException("Le code de la politique est requis");
        }

        return policyRepository.findByCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Politique de ressource introuvable : " + code.trim()));
    }

    @Override
    @Transactional(readOnly = true)
    public ResourcePolicy getPolicyForService(Long id) {
        if (id == null) {
            throw new BadRequestException("L'identifiant de la politique est requis");
        }

        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Politique de ressource introuvable : " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourcePolicyResponse> list(Pageable pageable) {
        Page<ResourcePolicyResponse> page = policyRepository.findAll(pageable)
                .map(policyMapper::toResponse);

        return new PaginatedResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ResourcePolicyResponse> search(String query) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Le texte de recherche est requis");
        }

        List<ResourcePolicyResponse> responses = policyRepository.basicSearch(query.trim()).stream()
                .map(policyMapper::toResponse)
                .toList();

        return new PaginatedResponse<>(new PageImpl<>(responses));
    }

    private List<String> validateForCreate(CreateResourcePolicyRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Demande de politique de ressource invalide : le corps de la demande est requis");
        } else {
            validateCommonFields(
                    errors,
                    request.getName(),
                    request.getDescription(),
                    request.getMinBookingDurationMinutes(),
                    request.getMaxBookingDurationMinutes(),
                    request.getMinBookingNoticeMinutes(),
                    request.getCancellationNoticeMinutes() != null ? request.getCancellationNoticeMinutes() : 1
            );
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Demande de politique de ressource invalide", errors);
        }

        return errors;
    }

    private List<String> validateForUpdate(UpdateResourcePolicyRequest request, ResourcePolicy currentPolicy) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Demande de politique de ressource invalide : le corps de la demande est requis");
        } else {
            validateCommonFields(
                    errors,
                    request.getName() != null ? request.getName() : currentPolicy.getName(),
                    request.getDescription() != null ? request.getDescription() : currentPolicy.getDescription(),
                    request.getMinBookingDurationMinutes() != null ? request.getMinBookingDurationMinutes() : currentPolicy.getMinBookingDurationMinutes(),
                    request.getMaxBookingDurationMinutes() != null ? request.getMaxBookingDurationMinutes() : currentPolicy.getMaxBookingDurationMinutes(),
                    request.getMinBookingNoticeMinutes() != null ? request.getMinBookingNoticeMinutes() : currentPolicy.getMinBookingNoticeMinutes(),
                    request.getCancellationNoticeMinutes() != null ? request.getCancellationNoticeMinutes() : currentPolicy.getCancellationNoticeMinutes()
            );
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Demande de politique de ressource invalide", errors);
        }

        return errors;
    }

    private void validateCommonFields(List<String> errors,
                                      String name,
                                      String description,
                                      Integer minBookingDurationMinutes,
                                      Integer maxBookingDurationMinutes,
                                      Integer minBookingNoticeMinutes,
                                      Integer cancellationNoticeMinutes) {
        if (!StringUtils.hasText(name) || !ValidationUtils.validateString(name.trim())) {
            errors.add("Demande de politique de ressource invalide : le nom n'est pas valide");
        }

        if (StringUtils.hasText(description) && !ValidationUtils.validateDescription(description.trim())) {
            errors.add("Demande de politique de ressource invalide : la description n'est pas valide");
        }

        if (minBookingDurationMinutes == null || minBookingDurationMinutes < 1) {
            errors.add("Demande de politique de ressource invalide : la durée minimale de réservation doit être supérieure à zéro");
        }

        if (maxBookingDurationMinutes == null || maxBookingDurationMinutes < 1) {
            errors.add("Demande de politique de ressource invalide : la durée maximale de réservation doit être supérieure à zéro");
        }

        if (minBookingDurationMinutes != null
                && maxBookingDurationMinutes != null
                && maxBookingDurationMinutes < minBookingDurationMinutes) {
            errors.add("Demande de politique de ressource invalide : la durée maximale de réservation doit être au moins égale à la durée minimale");
        }

        if (minBookingNoticeMinutes == null || minBookingNoticeMinutes < 0) {
            errors.add("Demande de politique de ressource invalide : le préavis minimal de réservation doit être positif ou nul");
        }

        if (cancellationNoticeMinutes == null || cancellationNoticeMinutes < 0) {
            errors.add("Demande de politique de ressource invalide : le préavis d'annulation doit être positif ou nul");
        }
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private boolean isValidCode(String code) {
        return StringUtils.hasText(code) && code.trim().toUpperCase().matches("^[A-Z0-9_-]+$");
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
