package com.sni.bokaticowork.features.contract.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.contract.dto.request.RequestContractSigningRequest;
import com.sni.bokaticowork.features.contract.dto.request.SignContractRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractSigningResponse;
import com.sni.bokaticowork.features.contract.enums.ContractStatus;
import com.sni.bokaticowork.features.contract.model.Contract;
import com.sni.bokaticowork.features.contract.model.ContractSigningToken;
import com.sni.bokaticowork.features.contract.repository.ContractSigningTokenRepository;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractSigningService;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ContractSigningServiceImpl implements ContractSigningService {

    private final ContractSigningTokenRepository tokenRepository;
    private final ContractService contractService;
    private final OutboxService outboxService;
    private final MemberInAppNotifier memberInAppNotifier;

    @Value("${app.contract.signing-token-hours:72}")
    private int signingTokenHours;

    @Value("${app.api-base-url:}")
    private String apiBaseUrl;

    @Override
    public ContractSigningResponse requestSigning(String contractCode, RequestContractSigningRequest request) {
        Contract contract = contractService.serviceByCode(contractCode);
        if (contract.getStatus() != ContractStatus.AWAITING_SIGNATURE) {
            throw new BadRequestException("Le contrat doit être en attente de signature");
        }

        tokenRepository.findByContractCodeAndRevokedFalseAndSignedAtIsNull(contractCode)
                .ifPresent(existing -> {
                    existing.setRevoked(true);
                    tokenRepository.save(existing);
                });

        ContractSigningToken token = ContractSigningToken.builder()
                .contract(contract)
                .contractCode(contract.getContractCode())
                .signerEmail(request.getSignerEmail().trim())
                .signerName(request.getSignerName().trim())
                .expiresAt(Instant.now().plus(Duration.ofHours(signingTokenHours)))
                .build();
        tokenRepository.save(token);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contractCode", contract.getContractCode());
        payload.put("contractTitle", contract.getTitle());
        payload.put("signerEmail", token.getSignerEmail());
        payload.put("signerName", token.getSignerName());
        payload.put("signingToken", token.getToken().toString());
        payload.put("signingUrl", buildSigningUrl(token.getToken()));
        payload.put("expiresAt", token.getExpiresAt().toString());
        payload.put("ownerType", contract.getOwnerType() != null ? contract.getOwnerType().name() : null);
        payload.put("ownerCode", contract.getOwnerCode());
        outboxService.publish("CONTRACT_SIGNING_REQUESTED", "CONTRACT", contract.getContractCode(), payload);
        memberInAppNotifier.notify("CONTRACT_SIGNING_REQUESTED", "CONTRACT", contract.getContractCode(),
                token.getSignerEmail(), token.getSignerName(), contract.getOwnerCode(),
                "Nouveau contrat a signer — " + contract.getTitle(),
                Map.of("contractCode", contract.getContractCode(), "contractTitle", contract.getTitle()));

        log.info("Signing request created for contract={} signer={}", contractCode, token.getSignerEmail());
        return toResponse(token, contract);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractSigningResponse getSigningDetails(UUID tokenUuid) {
        ContractSigningToken token = findValidToken(tokenUuid);
        return toResponse(token, token.getContract());
    }

    @Override
    public ContractSigningResponse sign(UUID tokenUuid, SignContractRequest request, String ipAddress, String userAgent) {
        if (request == null || !request.isAccepted()) {
            throw new BadRequestException("Vous devez accepter les termes du contrat pour signer");
        }

        ContractSigningToken token = findValidToken(tokenUuid);
        Contract contract = token.getContract();

        if (contract.getStatus() != ContractStatus.AWAITING_SIGNATURE) {
            throw new BadRequestException("Le contrat n'est plus en attente de signature");
        }

        token.setSignedAt(Instant.now());
        token.setIpAddress(ipAddress);
        token.setUserAgent(userAgent);
        token.setConsentText(request.getConsentText());
        tokenRepository.save(token);

        String documentCode = contract.getDraftDocumentCode();
        if (documentCode == null) {
            documentCode = contract.getContractCode();
        }
        contractService.markSigned(contract.getContractCode(), documentCode);

        Map<String, Object> payload = new HashMap<>();
        payload.put("contractCode", contract.getContractCode());
        payload.put("contractTitle", contract.getTitle());
        payload.put("signerEmail", token.getSignerEmail());
        payload.put("signerName", token.getSignerName());
        payload.put("ownerType", contract.getOwnerType() != null ? contract.getOwnerType().name() : null);
        payload.put("ownerCode", contract.getOwnerCode());
        outboxService.publish("CONTRACT_SIGNED_VIA_ESIGN", "CONTRACT", contract.getContractCode(), payload);
        memberInAppNotifier.notify("CONTRACT_SIGNED_VIA_ESIGN", "CONTRACT", contract.getContractCode(),
                token.getSignerEmail(), token.getSignerName(), contract.getOwnerCode(),
                "Contrat signe — " + contract.getTitle(),
                Map.of("contractCode", contract.getContractCode(), "contractTitle", contract.getTitle()));

        log.info("Contract {} signed by {} via e-signature", contract.getContractCode(), token.getSignerEmail());
        return toResponse(token, contract);
    }

    private ContractSigningToken findValidToken(UUID tokenUuid) {
        ContractSigningToken token = tokenRepository.findByToken(tokenUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Lien de signature introuvable"));
        if (Boolean.TRUE.equals(token.getRevoked())) {
            throw new BadRequestException("Ce lien de signature a été révoqué");
        }
        if (token.getSignedAt() != null) {
            throw new BadRequestException("Ce contrat a déjà été signé");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Ce lien de signature a expiré");
        }
        return token;
    }

    private String buildSigningUrl(UUID token) {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            return "/contracts/sign/" + token;
        }
        return apiBaseUrl + "/sni/api/v1/public/contracts/sign/" + token;
    }

    private ContractSigningResponse toResponse(ContractSigningToken token, Contract contract) {
        return new ContractSigningResponse(
                token.getToken(),
                token.getContractCode(),
                contract.getTitle(),
                token.getSignerEmail(),
                token.getSignerName(),
                token.getExpiresAt(),
                token.getSignedAt(),
                Boolean.TRUE.equals(token.getRevoked())
        );
    }
}
