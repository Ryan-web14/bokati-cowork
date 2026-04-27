package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementTransactionType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementLedger;
import com.sni.bokaticowork.features.subscription.repository.EntitlementLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class EntitlementLedgerWriter {

    private final EntitlementLedgerRepository ledgerRepository;

    public void write(EntitlementGrant grant,
                      EntitlementTransactionType transactionType,
                      BigDecimal quantity,
                      BigDecimal before,
                      BigDecimal after,
                      String referenceType,
                      String referenceId,
                      String idempotencyKey,
                      String reason) {
        ledgerRepository.save(EntitlementLedger.builder()
                .grant(grant)
                .transactionType(transactionType)
                .quantity(quantity)
                .beforeQuantity(before)
                .afterQuantity(after)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .idempotencyKey(idempotencyKey)
                .reason(reason)
                .build());
    }
}
