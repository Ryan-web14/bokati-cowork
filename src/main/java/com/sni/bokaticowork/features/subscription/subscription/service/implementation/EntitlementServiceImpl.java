package com.sni.bokaticowork.features.subscription.subscription.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementGrantResponse;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementOperationResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementTransactionType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.mapper.interfaces.SubscriptionEntitlementMapper;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.specification.criteria.EntitlementGrantSearchCriteria;
import com.sni.bokaticowork.features.subscription.repository.specification.specification.EntitlementGrantSpecification;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.subscription.service.support.EntitlementGrantIssuer;
import com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement.EntitlementBalanceReader;
import com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement.EntitlementGrantBalanceOperator;
import com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement.EntitlementReservationOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EntitlementServiceImpl implements EntitlementService {

    private final EntitlementGrantRepository grantRepository;
    private final SubscriptionEntitlementMapper responseMapper;
    private final EntitlementGrantIssuer grantIssuer;
    private final EntitlementBalanceReader balanceReader;
    private final EntitlementReservationOperator reservationOperator;
    private final EntitlementGrantBalanceOperator grantBalanceOperator;

    @Override
    public void grantForSubscription(Subscription subscription) {
        grantIssuer.grantForSubscription(subscription);
    }

    @Override
    public void grantForPass(Pass pass) {
        grantIssuer.grantForPass(pass);
    }

    @Override
    @Transactional(readOnly = true)
    public EntitlementOperationResponse check(EntitlementOperationRequest request) {
        return balanceReader.check(request);
    }

    @Override
    public EntitlementOperationResponse reserve(EntitlementOperationRequest request) {
        return reservationOperator.reserve(request);
    }

    @Override
    public EntitlementOperationResponse consume(EntitlementOperationRequest request) {
        return reservationOperator.consume(request);
    }

    @Override
    public EntitlementOperationResponse release(EntitlementOperationRequest request) {
        return reservationOperator.release(request);
    }

    @Override
    public EntitlementOperationResponse refund(EntitlementOperationRequest request) {
        return grantBalanceOperator.credit(request, EntitlementTransactionType.REFUND);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<EntitlementGrantResponse> list(EntitlementGrantSearchCriteria criteria, Pageable pageable) {
        return new PaginatedResponse<>(grantRepository.findAll(EntitlementGrantSpecification.search(criteria), pageable)
                .map(responseMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntitlementGrantResponse> balances(SubscriberType ownerType, String ownerCode) {
        if (ownerType == null || !StringUtils.hasText(ownerCode)) {
            throw new BadRequestException("Owner type and owner code are required");
        }
        return grantRepository.findActiveBalances(ownerType.name(), ownerCode.trim(), Instant.now())
                .stream()
                .map(responseMapper::toResponse)
                .toList();
    }

    @Override
    public int expireGrants() {
        return grantBalanceOperator.expireGrants();
    }

    @Override
    public int expireReservations() {
        return reservationOperator.expireReservations();
    }
}
