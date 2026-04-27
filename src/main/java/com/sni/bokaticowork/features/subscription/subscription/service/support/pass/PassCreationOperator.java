package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreatePassRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PassEntitlementRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PassTransaction;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.repository.PassEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.PassTransactionRepository;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionContractSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionOwnerResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PassCreationOperator {

    private final PassRepository passRepository;
    private final PassEntitlementRepository passEntitlementRepository;
    private final PassTransactionRepository passTransactionRepository;
    private final EntitlementDefinitionRepository entitlementDefinitionRepository;
    private final PlanVersionRepository planVersionRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionOwnerResolver ownerResolver;
    private final SubscriptionContractSupport contractSupport;
    private final @Lazy EntitlementService entitlementService;
    private final @Lazy SubscriptionService subscriptionService;

    public Pass create(CreatePassRequest request) {
        if (!StringUtils.hasText(request.ownerCode())) {
            throw new BadRequestException("Owner code is required");
        }
        SubscriptionOwnerResolver.Owner owner = ownerResolver.resolve(request.ownerType(), request.ownerCode().trim());
        Subscription subscription = StringUtils.hasText(request.subscriptionNumber())
                ? subscriptionService.getForService(request.subscriptionNumber())
                : null;
        PlanVersion planVersion = request.planVersionId() == null
                ? null
                : planVersionRepository.findById(request.planVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan version not found"));

        Pass pass = passRepository.save(Pass.builder()
                .passNumber(sequenceGenerator.next("pass"))
                .passType(request.passType())
                .ownerType(request.ownerType())
                .ownerCode(owner.code())
                .subscription(subscription)
                .planVersion(planVersion)
                .status(PassStatus.ACTIVE)
                .name(request.name().trim())
                .description(trim(request.description()))
                .validFrom(request.validFrom())
                .validUntil(request.validUntil())
                .transferable(Boolean.TRUE.equals(request.transferable()))
                .shareable(Boolean.TRUE.equals(request.shareable()))
                .maxUses(request.maxUses())
                .metadataJson(trim(request.metadataJson()))
                .build());

        if (request.entitlements() == null || request.entitlements().isEmpty()) {
            throw new BadRequestException("At least one pass entitlement is required");
        }
        request.entitlements().forEach(entitlement -> passEntitlementRepository.save(toPassEntitlement(pass, entitlement)));
        passTransactionRepository.save(PassTransaction.builder()
                .pass(pass)
                .transactionType("ISSUED")
                .referenceType("PASS")
                .referenceId(pass.getPassNumber())
                .build());
        entitlementService.grantForPass(pass);
        pass.setContractCode(contractSupport.createAndSignForPass(pass));
        passRepository.save(pass);
        return pass;
    }

    private PassEntitlement toPassEntitlement(Pass pass, PassEntitlementRequest request) {
        EntitlementDefinition definition = entitlementDefinitionRepository.findByCodeIgnoreCase(request.entitlementCode())
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement definition " + request.entitlementCode() + " not found"));
        boolean unlimited = Boolean.TRUE.equals(request.unlimited());
        if (!unlimited && request.quantity() == null) {
            throw new BadRequestException("Pass entitlement quantity is required when unlimited is false");
        }
        return PassEntitlement.builder()
                .pass(pass)
                .entitlementDefinition(definition)
                .quantity(unlimited ? null : request.quantity())
                .unlimited(unlimited)
                .validFrom(request.validFrom() == null ? pass.getValidFrom() : request.validFrom())
                .validUntil(request.validUntil() == null ? pass.getValidUntil() : request.validUntil())
                .build();
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
