package com.sni.bokaticowork.features.subscription.subscription.service.support.entitlement;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.EntitlementOperationRequest;
import com.sni.bokaticowork.features.subscription.subscription.dto.response.EntitlementOperationResponse;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementTransactionType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementLedgerRepository;
import com.sni.bokaticowork.features.subscription.subscription.service.support.EntitlementLedgerWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EntitlementGrantBalanceOperator {

    private final EntitlementGrantRepository grantRepository;
    private final EntitlementLedgerRepository ledgerRepository;
    private final EntitlementLedgerWriter ledgerWriter;
    private final EntitlementBalanceReader balanceReader;
    private final EntitlementRequestValidator validator;
    private final EntitlementQuotaAlertService quotaAlertService;

    public EntitlementOperationResponse debit(EntitlementOperationRequest request, EntitlementTransactionType type) {
        validator.operation(request);
        if (isDuplicate(request)) {
            return balanceReader.check(request);
        }

        BigDecimal remainingToDebit = request.quantity();
        List<EntitlementGrant> grants = balanceReader.usableGrants(request);

        if (balanceReader.hasUnlimitedGrant(grants)) {
            EntitlementGrant grant = firstUnlimited(grants);
            ledgerWriter.write(grant, type, request.quantity(), null, null, request.referenceType(), request.referenceId(), request.idempotencyKey(), request.reason());
            return new EntitlementOperationResponse(true, request.entitlementCode(), request.quantity(), null, "Unlimited entitlement applied");
        }

        BigDecimal available = balanceReader.availableQuantity(grants);
        if (available.compareTo(request.quantity()) < 0) {
            throw new ConflictException("entitlement", "insufficient balance");
        }

        for (EntitlementGrant candidate : grants) {
            if (remainingToDebit.signum() <= 0) {
                break;
            }
            EntitlementGrant grant = grantRepository.findLockedById(candidate.getId()).orElseThrow();
            BigDecimal before = grant.getQuantityRemaining();
            BigDecimal debit = before.min(remainingToDebit);
            BigDecimal after = before.subtract(debit);
            grant.setQuantityRemaining(after);
            if (after.signum() == 0) {
                grant.setStatus(EntitlementGrantStatus.DEPLETED);
            }
            grantRepository.save(grant);
            ledgerWriter.write(grant, type, debit, before, after, request.referenceType(), request.referenceId(), request.idempotencyKey(), request.reason());
            quotaAlertService.checkAndAlert(grant, after);
            remainingToDebit = remainingToDebit.subtract(debit);
        }

        return new EntitlementOperationResponse(true, request.entitlementCode(), request.quantity(), available.subtract(request.quantity()), type.name() + " completed");
    }

    public EntitlementOperationResponse credit(EntitlementOperationRequest request, EntitlementTransactionType type) {
        validator.operation(request);
        if (isDuplicate(request)) {
            return balanceReader.check(request);
        }

        List<EntitlementGrant> grants = balanceReader.usableGrants(request);
        if (grants.isEmpty()) {
            throw new BadRequestException("No active entitlement grant found to credit");
        }

        EntitlementGrant grant = grantRepository.findLockedById(grants.get(0).getId()).orElseThrow();
        if (Boolean.TRUE.equals(grant.getUnlimited())) {
            ledgerWriter.write(grant, type, request.quantity(), null, null, request.referenceType(), request.referenceId(), request.idempotencyKey(), request.reason());
            return new EntitlementOperationResponse(true, request.entitlementCode(), request.quantity(), null, "Unlimited entitlement credited");
        }

        BigDecimal before = grant.getQuantityRemaining();
        BigDecimal after = before.add(request.quantity());
        grant.setQuantityRemaining(after);
        grant.setStatus(EntitlementGrantStatus.ACTIVE);
        grantRepository.save(grant);
        ledgerWriter.write(grant, type, request.quantity(), before, after, request.referenceType(), request.referenceId(), request.idempotencyKey(), request.reason());

        return new EntitlementOperationResponse(true, request.entitlementCode(), request.quantity(), after, type.name() + " completed");
    }

    public int expireGrants() {
        List<EntitlementGrant> grants = grantRepository.findAllByStatusAndValidUntilBefore(EntitlementGrantStatus.ACTIVE.name(), Instant.now());
        for (EntitlementGrant grant : grants) {
            grant.setStatus(EntitlementGrantStatus.EXPIRED);
            grantRepository.save(grant);
            ledgerWriter.write(grant, EntitlementTransactionType.EXPIRE, grant.getQuantityRemaining(), grant.getQuantityRemaining(), BigDecimal.ZERO, "WORKER", "ENTITLEMENT_EXPIRY", null, "grant expired");
        }
        return grants.size();
    }

    private boolean isDuplicate(EntitlementOperationRequest request) {
        return StringUtils.hasText(request.idempotencyKey()) && ledgerRepository.existsByIdempotencyKey(request.idempotencyKey());
    }

    private EntitlementGrant firstUnlimited(List<EntitlementGrant> grants) {
        return grants.stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getUnlimited()))
                .findFirst()
                .orElseThrow();
    }
}
