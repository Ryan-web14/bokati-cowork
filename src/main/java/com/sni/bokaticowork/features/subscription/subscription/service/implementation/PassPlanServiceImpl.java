package com.sni.bokaticowork.features.subscription.subscription.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanPriceRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.PassPlanDtos;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PlanStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlan;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanPrice;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.PassPlanService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PassPlanServiceImpl implements PassPlanService {

    private final PassPlanRepository passPlanRepository;
    private final PassPlanVersionRepository passPlanVersionRepository;
    private final PassPlanPriceRepository passPlanPriceRepository;
    private final PassPlanEntitlementRepository passPlanEntitlementRepository;
    private final EntitlementDefinitionRepository entitlementDefinitionRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    // ── Plan operations ──

    @Override
    public PassPlanDtos.PassPlanResponse createPlan(PassPlanDtos.CreatePassPlanRequest request) {
        PassPlan plan = PassPlan.builder()
                .code(sequenceGenerator.next("pass_plan"))
                .name(request.name())
                .description(request.description())
                .passType(request.passType())
                .targetAudience(request.targetAudience())
                .visible(request.visible() != null ? request.visible() : Boolean.FALSE)
                .sortOrder(request.sortOrder())
                .requiredKycLevel(request.requiredKycLevel() != null ? request.requiredKycLevel() : 1)
                .build();

        return toResponse(passPlanRepository.save(plan));
    }

    @Override
    public PassPlanDtos.PassPlanResponse updatePlan(String planCode, PassPlanDtos.UpdatePassPlanRequest request) {
        PassPlan plan = findPlanByCode(planCode);

        if (StringUtils.hasText(request.name())) {
            plan.setName(request.name());
        }
        if (request.description() != null) {
            plan.setDescription(request.description());
        }
        if (request.visible() != null) {
            plan.setVisible(request.visible());
        }
        if (request.sortOrder() != null) {
            plan.setSortOrder(request.sortOrder());
        }
        if (request.requiredKycLevel() != null) {
            plan.setRequiredKycLevel(request.requiredKycLevel());
        }

        return toResponse(passPlanRepository.save(plan));
    }

    @Override
    @Transactional(readOnly = true)
    public PassPlanDtos.PassPlanResponse getPlan(String planCode) {
        return toResponse(findPlanByCode(planCode));
    }

    @Override
    public PassPlanDtos.PassPlanResponse publishPlan(String planCode) {
        PassPlan plan = findPlanByCode(planCode);
        if (plan.getStatus() != PlanStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT plans can be published. Current status: " + plan.getStatus());
        }
        plan.setStatus(PlanStatus.ACTIVE);
        return toResponse(passPlanRepository.save(plan));
    }

    @Override
    public PassPlanDtos.PassPlanResponse archivePlan(String planCode) {
        PassPlan plan = findPlanByCode(planCode);
        plan.setStatus(PlanStatus.ARCHIVED);
        return toResponse(passPlanRepository.save(plan));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<PassPlanDtos.PassPlanResponse> searchPlans(PassType passType, PlanStatus status,
                                                                        String searchText, Pageable pageable) {
        Specification<PassPlan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (passType != null) {
                predicates.add(cb.equal(root.get("passType"), passType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(searchText)) {
                String pattern = "%" + searchText.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PassPlanDtos.PassPlanResponse> page = passPlanRepository.findAll(spec, pageable)
                .map(this::toResponse);
        return new PaginatedResponse<>(page);
    }

    // ── Version operations ──

    @Override
    public PassPlanDtos.PassPlanVersionResponse createVersion(String planCode,
                                                               PassPlanDtos.CreatePassPlanVersionRequest request) {
        PassPlan plan = findPlanByCode(planCode);
        int nextVersion = passPlanVersionRepository.countByPlan(plan) + 1;

        PassPlanVersion version = PassPlanVersion.builder()
                .plan(plan)
                .versionNumber(nextVersion)
                .name(request.name())
                .description(request.description())
                .duration(request.duration())
                .durationUnit(request.durationUnit())
                .maxUses(request.maxUses())
                .autoRenewable(request.autoRenewable() != null ? request.autoRenewable() : Boolean.FALSE)
                .requiredKycLevel(request.requiredKycLevel() != null ? request.requiredKycLevel() : 1)
                .effectiveFrom(request.effectiveFrom())
                .effectiveTo(request.effectiveTo())
                .termsJson(request.termsJson())
                .build();

        return toVersionResponse(passPlanVersionRepository.save(version));
    }

    @Override
    @Transactional(readOnly = true)
    public PassPlanDtos.PassPlanVersionResponse getVersion(Long versionId) {
        return toVersionResponse(findVersionById(versionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassPlanDtos.PassPlanVersionResponse> listVersions(String planCode) {
        PassPlan plan = findPlanByCode(planCode);
        return passPlanVersionRepository.findAllByPlanOrderByVersionNumberDesc(plan).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Override
    public PassPlanDtos.PassPlanVersionResponse publishVersion(Long versionId) {
        PassPlanVersion version = findVersionById(versionId);
        if (version.getStatus() != PlanStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT versions can be published. Current status: " + version.getStatus());
        }
        version.setStatus(PlanStatus.ACTIVE);
        return toVersionResponse(passPlanVersionRepository.save(version));
    }

    // ── Price operations ──

    @Override
    public PassPlanDtos.PassPlanPriceResponse setPrice(Long versionId, PassPlanDtos.SetPassPlanPriceRequest request) {
        PassPlanVersion version = findVersionById(versionId);

        PassPlanPrice price = passPlanPriceRepository
                .findByPassVersionAndCurrency(version, request.currency())
                .orElseGet(() -> PassPlanPrice.builder()
                        .passVersion(version)
                        .currency(request.currency())
                        .build());

        price.setAmount(request.amount());
        price.setSetupFee(request.setupFee() != null ? request.setupFee() : BigDecimal.ZERO);
        price.setDepositAmount(request.depositAmount() != null ? request.depositAmount() : BigDecimal.ZERO);
        price.setTaxIncluded(request.taxIncluded() != null ? request.taxIncluded() : Boolean.TRUE);
        price.setTaxCode(request.taxCode());

        return toPriceResponse(passPlanPriceRepository.save(price));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassPlanDtos.PassPlanPriceResponse> listPrices(Long versionId) {
        PassPlanVersion version = findVersionById(versionId);
        return passPlanPriceRepository.findAllByPassVersion(version).stream()
                .map(this::toPriceResponse)
                .toList();
    }

    // ── Entitlement operations ──

    @Override
    public PassPlanDtos.PassPlanEntitlementResponse addEntitlement(Long versionId,
                                                                    PassPlanDtos.AddPassPlanEntitlementRequest request) {
        PassPlanVersion version = findVersionById(versionId);

        EntitlementDefinition definition = entitlementDefinitionRepository
                .findByCodeIgnoreCase(request.entitlementCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "EntitlementDefinition", "code", request.entitlementCode()));

        PassPlanEntitlement entitlement = PassPlanEntitlement.builder()
                .passVersion(version)
                .entitlementDefinition(definition)
                .quantity(request.quantity())
                .unlimited(request.unlimited() != null ? request.unlimited() : Boolean.FALSE)
                .rolloverAllowed(request.rolloverAllowed() != null ? request.rolloverAllowed() : Boolean.FALSE)
                .rolloverLimit(request.rolloverLimit())
                .validForDays(request.validForDays())
                .priority(request.priority() != null ? request.priority() : 100)
                .build();

        return toEntitlementResponse(passPlanEntitlementRepository.save(entitlement));
    }

    @Override
    public void removeEntitlement(Long entitlementId) {
        if (!passPlanEntitlementRepository.existsById(entitlementId)) {
            throw new ResourceNotFoundException("PassPlanEntitlement", "id", entitlementId);
        }
        passPlanEntitlementRepository.deleteById(entitlementId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassPlanDtos.PassPlanEntitlementResponse> listEntitlements(Long versionId) {
        PassPlanVersion version = findVersionById(versionId);
        return passPlanEntitlementRepository.findAllByPassVersion(version).stream()
                .map(this::toEntitlementResponse)
                .toList();
    }

    // ── Private helpers ──

    private PassPlan findPlanByCode(String planCode) {
        return passPlanRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("PassPlan", "code", planCode));
    }

    private PassPlanVersion findVersionById(Long versionId) {
        return passPlanVersionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("PassPlanVersion", "id", versionId));
    }

    private PassPlanDtos.PassPlanResponse toResponse(PassPlan plan) {
        return new PassPlanDtos.PassPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getPassType(),
                plan.getTargetAudience(),
                plan.getStatus(),
                plan.getVisible(),
                plan.getSortOrder(),
                plan.getRequiredKycLevel(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private PassPlanDtos.PassPlanVersionResponse toVersionResponse(PassPlanVersion version) {
        return new PassPlanDtos.PassPlanVersionResponse(
                version.getId(),
                version.getPlan().getCode(),
                version.getVersionNumber(),
                version.getName(),
                version.getDescription(),
                version.getStatus(),
                version.getDuration(),
                version.getDurationUnit(),
                version.getMaxUses(),
                version.getAutoRenewable(),
                version.getRequiredKycLevel(),
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }

    private PassPlanDtos.PassPlanPriceResponse toPriceResponse(PassPlanPrice price) {
        return new PassPlanDtos.PassPlanPriceResponse(
                price.getId(),
                price.getPassVersion().getId(),
                price.getCurrency(),
                price.getAmount(),
                price.getSetupFee(),
                price.getDepositAmount(),
                price.getTaxIncluded(),
                price.getTaxCode(),
                price.getCreatedAt()
        );
    }

    private PassPlanDtos.PassPlanEntitlementResponse toEntitlementResponse(PassPlanEntitlement entitlement) {
        return new PassPlanDtos.PassPlanEntitlementResponse(
                entitlement.getId(),
                entitlement.getPassVersion().getId(),
                entitlement.getEntitlementDefinition().getCode(),
                entitlement.getEntitlementDefinition().getName(),
                entitlement.getQuantity(),
                entitlement.getUnlimited(),
                entitlement.getRolloverAllowed(),
                entitlement.getRolloverLimit(),
                entitlement.getValidForDays(),
                entitlement.getPriority(),
                entitlement.getCreatedAt()
        );
    }
}
