package com.sni.bokaticowork.features.document.documentMaster.service.implementation;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.repository.ContractRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSignatureStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentSignature;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentSignatureRepository;
import com.sni.bokaticowork.features.subscription.addon.enums.SubscriptionAddonStatus;
import com.sni.bokaticowork.features.subscription.addon.model.SubscriptionAddon;
import com.sni.bokaticowork.features.subscription.addon.repository.SubscriptionAddonRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.service.support.pass.PassLifecycleOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
class DocumentRejectionCascadeService {

    private static final String PAYMENT_POLICY = "PAYMENTS_UNCHANGED_REFUND_REQUIRED";

    private final ContractRepository contractRepository;
    private final DocumentSignatureRepository signatureRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PassRepository passRepository;
    private final SubscriptionAddonRepository addonRepository;
    private final EntitlementGrantRepository entitlementGrantRepository;
    private final PassLifecycleOperator passLifecycleOperator;
    private final OutboxService outboxService;

    void cascade(Document document, Long actorId, String reason) {
        declinePendingSignatures(document);
        cancelLinkedContracts(document, actorId, reason);
    }

    private void declinePendingSignatures(Document document) {
        List<DocumentSignature> signatures = signatureRepository.findAllByDocumentAndSignatureStatus(
                document,
                DocumentSignatureStatus.PENDING
        );
        if (signatures.isEmpty()) {
            return;
        }
        signatures.forEach(signature -> signature.setSignatureStatus(DocumentSignatureStatus.DECLINED));
        signatureRepository.saveAll(signatures);
        log.info("Declined {} pending signature request(s) after document rejection {}", signatures.size(), document.getCode());
    }

    private void cancelLinkedContracts(Document document, Long actorId, String reason) {
        Map<Long, Contract> contracts = new LinkedHashMap<>();
        contractRepository.findAllByDraftDocumentCodeOrSignedDocumentCode(document.getCode(), document.getCode())
                .forEach(contract -> contracts.put(contract.getId(), contract));
        if (document.getOwnerType() == DocumentOwnerType.CONTRACT && document.getOwnerId() != null) {
            contractRepository.findById(document.getOwnerId())
                    .ifPresent(contract -> contracts.put(contract.getId(), contract));
        }

        if (contracts.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        String cancellationReason = cancellationReason(document, reason);
        contracts.values().forEach(contract -> {
            if (isTerminal(contract.getStatus())) {
                return;
            }
            contract.setStatus(ContractStatus.CANCELLED);
            contract.setTerminatedAt(now);
            contract.setTerminationReason(cancellationReason);
            contractRepository.save(contract);
            publishContractCancellationEvent(contract, document, actorId, cancellationReason);
            cancelContractDependents(contract, document, actorId, cancellationReason);
        });
    }

    private void cancelContractDependents(Contract contract, Document document, Long actorId, String reason) {
        cancelLinkedSubscriptions(contract, document, actorId, reason);
        cancelLinkedPasses(contract, document, actorId, reason);
        cancelLinkedAddons(contract, document, actorId, reason);
    }

    private void cancelLinkedSubscriptions(Contract contract, Document document, Long actorId, String reason) {
        for (Subscription subscription : subscriptionRepository.findAllByContractCode(contract.getContractCode())) {
            if (subscription.getStatus() == SubscriptionStatus.CANCELLED || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
                continue;
            }
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancelAtPeriodEnd(Boolean.FALSE);
            subscription.setCancelledAt(Instant.now());
            subscription.setCancellationReason(reason);
            subscriptionRepository.save(subscription);
            cancelActiveSubscriptionGrants(subscription);
            publishDependentCancellationEvent(
                    "SUBSCRIPTION_CANCELLED_AFTER_CONTRACT_DOCUMENT_REJECTION",
                    "SUBSCRIPTION",
                    subscription.getSubscriptionNumber(),
                    contract,
                    document,
                    actorId,
                    reason
            );
        }
    }

    private void cancelActiveSubscriptionGrants(Subscription subscription) {
        List<EntitlementGrant> grants = entitlementGrantRepository.findAllBySubscription(subscription.getId()).stream()
                .filter(grant -> grant.getStatus() == EntitlementGrantStatus.ACTIVE)
                .toList();
        grants.forEach(grant -> grant.setStatus(EntitlementGrantStatus.CANCELLED));
        entitlementGrantRepository.saveAll(grants);
    }

    private void cancelLinkedPasses(Contract contract, Document document, Long actorId, String reason) {
        for (Pass pass : passRepository.findAllByContractCode(contract.getContractCode())) {
            if (pass.getStatus() == PassStatus.CANCELLED
                    || pass.getStatus() == PassStatus.EXPIRED
                    || pass.getStatus() == PassStatus.CONSUMED) {
                continue;
            }
            passLifecycleOperator.cancel(pass, reason);
            publishDependentCancellationEvent(
                    "PASS_CANCELLED_AFTER_CONTRACT_DOCUMENT_REJECTION",
                    "PASS",
                    pass.getPassNumber(),
                    contract,
                    document,
                    actorId,
                    reason
            );
        }
    }

    private void cancelLinkedAddons(Contract contract, Document document, Long actorId, String reason) {
        for (SubscriptionAddon addon : addonRepository.findAllByContractCode(contract.getContractCode())) {
            if (addon.getStatus() == SubscriptionAddonStatus.CANCELLED || addon.getStatus() == SubscriptionAddonStatus.EXPIRED) {
                continue;
            }
            addon.setStatus(SubscriptionAddonStatus.CANCELLED);
            addon.setEndsAt(java.time.LocalDate.now());
            addonRepository.save(addon);
            publishDependentCancellationEvent(
                    "ADDON_CANCELLED_AFTER_CONTRACT_DOCUMENT_REJECTION",
                    "SUBSCRIPTION_ADDON",
                    String.valueOf(addon.getId()),
                    contract,
                    document,
                    actorId,
                    reason
            );
        }
    }

    private boolean isTerminal(ContractStatus status) {
        return status == ContractStatus.CANCELLED
                || status == ContractStatus.TERMINATED
                || status == ContractStatus.EXPIRED;
    }

    private String cancellationReason(Document document, String reason) {
        String suffix = StringUtils.hasText(reason) ? " - " + reason.trim() : "";
        return "Document rejected: " + document.getCode() + suffix;
    }

    private void publishContractCancellationEvent(Contract contract, Document document, Long actorId, String reason) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("contractCode", contract.getContractCode());
        payload.put("ownerType", contract.getOwnerType());
        payload.put("ownerCode", contract.getOwnerCode());
        payload.put("status", contract.getStatus());
        payload.put("documentCode", document.getCode());
        payload.put("documentTypeCode", document.getDocumentType() == null ? null : document.getDocumentType().getCode());
        payload.put("actorId", actorId);
        payload.put("reason", reason);
        payload.put("paymentPolicy", PAYMENT_POLICY);
        outboxService.publish(
                "CONTRACT_CANCELLED_AFTER_DOCUMENT_REJECTION",
                "CONTRACT",
                contract.getContractCode(),
                payload
        );
    }

    private void publishDependentCancellationEvent(
            String eventType,
            String aggregateType,
            String aggregateId,
            Contract contract,
            Document document,
            Long actorId,
            String reason
    ) {
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("contractCode", contract.getContractCode());
        payload.put("documentCode", document.getCode());
        payload.put("actorId", actorId);
        payload.put("reason", reason);
        payload.put("paymentPolicy", PAYMENT_POLICY);
        outboxService.publish(eventType, aggregateType, aggregateId, payload);
    }
}
