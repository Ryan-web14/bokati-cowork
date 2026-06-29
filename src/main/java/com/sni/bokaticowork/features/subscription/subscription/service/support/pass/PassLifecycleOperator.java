package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassEventType;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassTransaction;
import com.sni.bokaticowork.features.subscription.subscription.service.interfaces.EntitlementService;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.PassTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PassLifecycleOperator {

    private final PassRepository passRepository;
    private final PassTransactionRepository passTransactionRepository;
    private final EntitlementGrantRepository entitlementGrantRepository;
    private final PassEventWriter eventWriter;
    private final PassEmailNotifier emailNotifier;
    private final PassBillingSupport billingSupport;
    private final @Lazy EntitlementService entitlementService;

    public Pass activate(Pass pass, String reason, String changedBy) {
        if (pass.getStatus() == PassStatus.ACTIVE) return pass;
        if (pass.getStatus() != PassStatus.PENDING_ACTIVATION) {
            throw new BadRequestException("Cannot activate pass " + pass.getPassNumber()
                    + " in status " + pass.getStatus());
        }
        PassStatus prev = pass.getStatus();
        pass.setStatus(PassStatus.ACTIVE);
        passRepository.save(pass);

        entitlementService.grantForPass(pass);
        billingSupport.upsertRenewalSchedule(pass);

        eventWriter.writeHistory(pass, prev, PassStatus.ACTIVE, reason, changedBy);
        eventWriter.writeEvent(pass, PassEventType.PASS_ACTIVATED, null);
        eventWriter.writeEvent(pass, PassEventType.ENTITLEMENTS_GRANTED, null);
        emailNotifier.notifyActivated(pass);

        if (Boolean.TRUE.equals(pass.getAutoRenew())) {
            eventWriter.writeEvent(pass, PassEventType.RENEWAL_SCHEDULED, null);
        }

        passTransactionRepository.save(PassTransaction.builder()
                .pass(pass).transactionType("ACTIVATED")
                .referenceType("PAYMENT").referenceId(reason).build());
        return pass;
    }

    public Pass markPastDue(Pass pass, String reason) {
        PassStatus prev = pass.getStatus();
        pass.setStatus(PassStatus.PAST_DUE);
        passRepository.save(pass);
        eventWriter.writeHistory(pass, prev, PassStatus.PAST_DUE, reason, "SYSTEM");
        eventWriter.writeEvent(pass, PassEventType.PASS_PAST_DUE, null);
        passTransactionRepository.save(PassTransaction.builder()
                .pass(pass).transactionType("PAST_DUE")
                .referenceType("RENEWAL").referenceId(reason).build());
        return pass;
    }

    public Pass cancel(Pass pass, String reason) {
        pass.setStatus(PassStatus.CANCELLED);
        pass.setCancelledAt(Instant.now());
        pass.setCancellationReason(reason);
        passRepository.save(pass);
        entitlementGrantRepository.findAllByOwnerTypeAndOwnerCodeAndStatus(pass.getOwnerType().name(), pass.getOwnerCode(), EntitlementGrantStatus.ACTIVE.name())
                .stream()
                .filter(grant -> grant.getPass() != null && grant.getPass().getId().equals(pass.getId()))
                .forEach(grant -> {
                    grant.setStatus(EntitlementGrantStatus.CANCELLED);
                    entitlementGrantRepository.save(grant);
                });
        eventWriter.writeHistory(pass, PassStatus.ACTIVE, PassStatus.CANCELLED, reason, "SYSTEM");
        eventWriter.writeEvent(pass, PassEventType.PASS_CANCELLED, null);
        emailNotifier.notifyCancelled(pass, reason);
        passTransactionRepository.save(PassTransaction.builder()
                .pass(pass)
                .transactionType("CANCELLED")
                .referenceType("PASS")
                .referenceId(pass.getPassNumber())
                .payloadJson(StringUtils.hasText(reason) ? "{\"reason\":\"" + reason.trim().replace("\"", "'") + "\"}" : null)
                .build());
        return pass;
    }

    public int expirePasses() {
        List<String> expirableStatuses = List.of(
                PassStatus.ACTIVE.name(),
                PassStatus.PENDING_ACTIVATION.name(),
                PassStatus.PAST_DUE.name()
        );
        List<Pass> passes = passRepository.findAllByStatusInAndValidUntilBefore(expirableStatuses, Instant.now());
        passes.forEach(pass -> {
            PassStatus prev = pass.getStatus();
            pass.setStatus(PassStatus.EXPIRED);
            passRepository.save(pass);
            eventWriter.writeHistory(pass, prev, PassStatus.EXPIRED, "Expiration automatique", "WORKER");
            eventWriter.writeEvent(pass, PassEventType.PASS_EXPIRED, null);
            passTransactionRepository.save(PassTransaction.builder()
                    .pass(pass)
                    .transactionType("EXPIRED")
                    .referenceType("WORKER")
                    .referenceId("PASS_EXPIRY")
                    .build());
        });
        return passes.size();
    }
}
