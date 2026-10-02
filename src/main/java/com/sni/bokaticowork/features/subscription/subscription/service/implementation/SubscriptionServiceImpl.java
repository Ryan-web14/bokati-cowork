package com.sni.bokaticowork.features.subscription.subscription.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.PauseSubscriptionRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.SubscriptionStatusChangeRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.BillingScheduleResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionHistoryResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.SubscriptionResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionEntitlementMapper;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionLifecycleMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionStatusHistoryRepository;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.SubscriptionSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.specification.SubscriptionSpecification;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionBillingSupport;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionCreationOperator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.subscription.SubscriptionLifecycleOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionStatusHistoryRepository historyRepository;
    private final EntitlementGrantRepository entitlementGrantRepository;
    private final SubscriptionLifecycleMapper responseMapper;
    private final SubscriptionEntitlementMapper entitlementMapper;
    private final SubscriptionBillingSupport billingSupport;
    private final SubscriptionCreationOperator creationOperator;
    private final SubscriptionLifecycleOperator lifecycleOperator;

    @Override
    public SubscriptionResponse create(CreateSubscriptionRequest request) {
        return responseMapper.toResponse(creationOperator.create(request));
    }

    @Override
    public SubscriptionResponse activate(String subscriptionNumber, SubscriptionStatusChangeRequest request) {
        Subscription subscription = getForService(subscriptionNumber);
        lifecycleOperator.activate(
                subscription,
                lifecycleOperator.reason(request, "Manual activation"),
                lifecycleOperator.actor(request)
        );
        return responseMapper.toResponse(subscription);
    }

    @Override
    public SubscriptionResponse suspend(String subscriptionNumber, SubscriptionStatusChangeRequest request) {
        return responseMapper.toResponse(lifecycleOperator.suspend(getForService(subscriptionNumber), request));
    }

    @Override
    public SubscriptionResponse pause(String subscriptionNumber, PauseSubscriptionRequest request) {
        return responseMapper.toResponse(lifecycleOperator.pause(getForService(subscriptionNumber), request));
    }

    @Override
    public SubscriptionResponse resume(String subscriptionNumber, SubscriptionStatusChangeRequest request) {
        return responseMapper.toResponse(lifecycleOperator.resume(getForService(subscriptionNumber), request));
    }

    @Override
    public SubscriptionResponse cancel(String subscriptionNumber, SubscriptionStatusChangeRequest request) {
        return responseMapper.toResponse(lifecycleOperator.cancel(getForService(subscriptionNumber), request));
    }

    @Override
    public SubscriptionResponse renew(String subscriptionNumber) {
        Subscription subscription = getForService(subscriptionNumber);
        lifecycleOperator.renew(subscription);
        return responseMapper.toResponse(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription getForService(String subscriptionNumber) {
        if (!StringUtils.hasText(subscriptionNumber)) {
            throw new BadRequestException("Subscription number is required");
        }
        return subscriptionRepository.findBySubscriptionNumber(subscriptionNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription " + subscriptionNumber + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse get(String subscriptionNumber) {
        return responseMapper.toResponse(getForService(subscriptionNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse current(SubscriberType subscriberType, String subscriberCode) {
        if (subscriberType == null || !StringUtils.hasText(subscriberCode)) {
            throw new BadRequestException("Subscriber type and subscriber code are required");
        }
        return subscriptionRepository.findCurrentActive(subscriberType.name(), subscriberCode.trim(), LocalDate.now())
                .map(responseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No current active subscription found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<SubscriptionResponse> list(SubscriptionSearchCriteria criteria, Pageable pageable) {
        return new PaginatedResponse<>(subscriptionRepository.findAll(SubscriptionSpecification.search(criteria), pageable)
                .map(responseMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntitlementGrantResponse> listEntitlements(String subscriptionNumber) {
        Subscription subscription = getForService(subscriptionNumber);
        return entitlementGrantRepository.findAllBySubscription(subscription.getId()).stream()
                .map(entitlementMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionHistoryResponse> history(String subscriptionNumber) {
        return historyRepository.findAllBySubscriptionOrderByChangedAtAsc(getForService(subscriptionNumber).getId())
                .stream()
                .map(responseMapper::toHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BillingScheduleResponse billingSchedule(String subscriptionNumber) {
        return billingSupport.getBillingSchedule(getForService(subscriptionNumber));
    }

    /**
     * Les balayages ne portent pas de transaction · chaque abonnement a la sienne.
     *
     * <p>La classe est transactionnelle, ce qui enfermait tout le balayage dans une seule
     * transaction : un abonnement en erreur annulait les renouvellements deja ecrits. Sans
     * transaction ici, l'echec d'un abonnement ne peut plus defaire celui du voisin.</p>
     */
    @Override
    public boolean realignOnLatePayment(String subscriptionNumber, java.time.LocalDate paidOn) {
        return lifecycleOperator.realignOnLatePayment(getForService(subscriptionNumber), paidOn);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int renewDueSubscriptions() {
        return lifecycleOperator.renewDueSubscriptions();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int cancelEndedSubscriptions() {
        return lifecycleOperator.cancelEndedSubscriptions();
    }

    @Override
    public int repairActiveSubscriptionsWithoutGrants() {
        return lifecycleOperator.repairActiveSubscriptionsWithoutGrants();
    }
}
