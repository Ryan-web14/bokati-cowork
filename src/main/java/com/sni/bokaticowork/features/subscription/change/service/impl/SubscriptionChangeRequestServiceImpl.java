package com.sni.bokaticowork.features.subscription.change.service.impl;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.change.dto.CreateSubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.dto.SubscriptionChangeResponse;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeEffectivePolicy;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeStatus;
import com.sni.bokaticowork.features.subscription.change.enums.SubscriptionChangeType;
import com.sni.bokaticowork.features.subscription.change.mapper.interfaces.SubscriptionChangeMapper;
import com.sni.bokaticowork.features.subscription.change.model.SubscriptionChangeRequest;
import com.sni.bokaticowork.features.subscription.change.repository.SubscriptionChangeRequestRepository;
import com.sni.bokaticowork.features.subscription.change.repository.specification.SubscriptionChangeCriteria;
import com.sni.bokaticowork.features.subscription.change.repository.specification.SubscriptionChangeSpecification;
import com.sni.bokaticowork.features.subscription.change.service.SubscriptionChangeRequestService;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.PlanVersionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionChangeRequestServiceImpl implements SubscriptionChangeRequestService {

    private final SubscriptionChangeRequestRepository changeRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanVersionRepository planVersionRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final SubscriptionChangeMapper changeMapper;

    @Override
    public SubscriptionChangeResponse request(String subscriptionNumber, CreateSubscriptionChangeRequest request) {
        Subscription subscription = subscriptionService.getForService(subscriptionNumber);
        PlanVersion targetPlan = request.targetPlanVersionId() == null
                ? null
                : planVersionRepository.findById(request.targetPlanVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Target plan version not found"));

        SubscriptionChangeRequest change = changeMapper.toEntity(request);
        change.setChangeNumber(sequenceGenerator.next("subscription_change"));
        change.setSubscription(subscription);
        change.setCurrentPlanVersion(subscription.getPlanVersion());
        change.setTargetPlanVersion(targetPlan);
        change.setEffectiveDate(resolveEffectiveDate(subscription, request.effectivePolicy(), request.effectiveDate()));
        return changeMapper.toResponse(changeRepository.save(change));
    }

    @Override
    public SubscriptionChangeResponse approve(String changeNumber, String approvedBy) {
        SubscriptionChangeRequest change = getChange(changeNumber);
        change.setStatus(SubscriptionChangeStatus.APPROVED);
        change.setApprovedBy(approvedBy == null || approvedBy.isBlank() ? null : approvedBy.trim());
        return changeMapper.toResponse(changeRepository.save(change));
    }

    @Override
    public SubscriptionChangeResponse apply(String changeNumber) {
        SubscriptionChangeRequest change = getChange(changeNumber);
        if (change.getStatus() != SubscriptionChangeStatus.APPROVED && change.getStatus() != SubscriptionChangeStatus.REQUESTED) {
            throw new ConflictException("subscription change", "only REQUESTED or APPROVED changes can be applied");
        }
        if (change.getTargetPlanVersion() != null) {
            Subscription subscription = change.getSubscription();
            subscription.setPlanVersion(change.getTargetPlanVersion());
            subscriptionRepository.save(subscription);
        }
        change.setStatus(SubscriptionChangeStatus.APPLIED);
        change.setAppliedAt(Instant.now());
        return changeMapper.toResponse(changeRepository.save(change));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SubscriptionChangeResponse> list(String subscriptionNumber, SubscriptionChangeType changeType, SubscriptionChangeStatus status, Pageable pageable) {
        return new PaginatedResponse<>(changeRepository.findAll(
                SubscriptionChangeSpecification.search(SubscriptionChangeCriteria.builder()
                        .subscriptionNumber(subscriptionNumber)
                        .changeType(changeType)
                        .status(status)
                        .build()),
                pageable
        ).map(changeMapper::toResponse));
    }

    @Override
    public int applyDueChanges() {
        List<SubscriptionChangeRequest> changes = changeRepository.findApprovedDueChanges(LocalDate.now());
        changes.forEach(change -> apply(change.getChangeNumber()));
        return changes.size();
    }

    private SubscriptionChangeRequest getChange(String changeNumber) {
        return changeRepository.findByChangeNumber(changeNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription change " + changeNumber + " not found"));
    }

    private LocalDate resolveEffectiveDate(Subscription subscription, SubscriptionChangeEffectivePolicy policy, LocalDate requestedDate) {
        if (policy == SubscriptionChangeEffectivePolicy.CUSTOM_DATE) {
            return requestedDate;
        }
        if (policy == SubscriptionChangeEffectivePolicy.NEXT_BILLING_PERIOD) {
            return subscription.getNextBillingDate();
        }
        return LocalDate.now();
    }

}
