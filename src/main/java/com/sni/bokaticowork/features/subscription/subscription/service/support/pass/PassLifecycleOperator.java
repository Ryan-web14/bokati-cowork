package com.sni.bokaticowork.features.subscription.subscription.service.support.pass;

import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PassTransaction;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.PassTransactionRepository;
import lombok.RequiredArgsConstructor;
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

    public Pass cancel(Pass pass, String reason) {
        pass.setStatus(PassStatus.CANCELLED);
        passRepository.save(pass);
        entitlementGrantRepository.findAllByOwnerTypeAndOwnerCodeAndStatus(pass.getOwnerType().name(), pass.getOwnerCode(), EntitlementGrantStatus.ACTIVE.name())
                .stream()
                .filter(grant -> grant.getPass() != null && grant.getPass().getId().equals(pass.getId()))
                .forEach(grant -> {
                    grant.setStatus(EntitlementGrantStatus.CANCELLED);
                    entitlementGrantRepository.save(grant);
                });
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
        List<Pass> passes = passRepository.findAllByStatusAndValidUntilBefore(PassStatus.ACTIVE.name(), Instant.now());
        passes.forEach(pass -> {
            pass.setStatus(PassStatus.EXPIRED);
            passRepository.save(pass);
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
