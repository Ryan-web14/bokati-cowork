package com.sni.bokaticowork.features.payment.service.pawaypay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyProviderOptionResponse;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.PawapayDeposit;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationRequest;
import com.sni.bokaticowork.features.payment.provider.MobileMoneyInitiationResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayFailureCodes;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayDepositProvider;
import com.sni.bokaticowork.features.payment.provider.pawaypay.PawapayProperties;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayCallbackPayload;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositRequest;
import com.sni.bokaticowork.features.payment.repository.PawapayDepositRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PawapayDepositService {

    private static final int CUSTOMER_MESSAGE_MAX_LENGTH = 22;
    private static final TypeReference<List<Map<String, Object>>> METADATA_TYPE = new TypeReference<>() {
    };

    private final PawapayDepositRepository repository;
    private final PawapayProperties properties;
    private final ObjectMapper objectMapper;
    private final PaymentMapper paymentMapper;

    public PawapayDeposit prepareDeposit(PaymentIntent intent,
                                         PaymentTransaction transaction,
                                         InitiateMobileMoneyDepositRequest request) {
        String depositId = UUID.randomUUID().toString();
        String provider = request.correspondent().providerCode();
        String phoneNumber = msisdn(request);
        String clientReferenceId = transaction.getTransactionNumber();
        String customerMessage = customerMessage(intent);
        String callbackUrl = properties.getCallbackBaseUrl()
                + ApiPath.V1
                + PawapayDepositProvider.PAWAYPAY_CALLBACK_PATH;
        List<Map<String, Object>> metadata = sanitizeMetadata(buildMetadata(intent, transaction, request));

        PawapayDeposit deposit = PawapayDeposit.builder()
                .depositId(depositId)
                .paymentIntent(intent)
                .paymentTransaction(transaction)
                .intentNumber(intent.getIntentNumber())
                .transactionNumber(transaction.getTransactionNumber())
                .customerType(intent.getCustomerType())
                .customerCode(intent.getCustomerCode())
                .payerType("MMO")
                .phoneNumber(phoneNumber)
                .provider(provider)
                .amount(transaction.getAmount())
                .currency(intent.getCurrency())
                .clientReferenceId(clientReferenceId)
                .customerMessage(customerMessage)
                .callbackUrl(callbackUrl)
                .metadataJson(writeJson(metadata))
                .status("CREATED")
                .build();

        deposit.setRequestPayloadJson(writeJson(toPawapayRequest(deposit, metadata)));
        return repository.save(deposit);
    }

    public MobileMoneyInitiationRequest toProviderRequest(PawapayDeposit deposit) {
        return new MobileMoneyInitiationRequest(
                deposit.getDepositId(),
                deposit.getIntentNumber(),
                deposit.getTransactionNumber(),
                deposit.getCustomerCode(),
                deposit.getPhoneNumber(),
                deposit.getAmount(),
                deposit.getCurrency(),
                deposit.getCallbackUrl(),
                deposit.getProvider(),
                deposit.getClientReferenceId(),
                deposit.getCustomerMessage(),
                metadata(deposit)
        );
    }

    /**
     * Ce que l'operateur a repondu a l'initiation · et quand on le relira.
     *
     * <p>Sans reponse ({@code UNKNOWN}), le depot n'est pas en echec : il est
     * {@code SUBMITTED_UNCONFIRMED} et sera relu dans une minute. Un refus explicite porte le code
     * de l'operateur, la phrase qu'on dit au client, et s'il peut reessayer.</p>
     */
    public PawapayDeposit markInitiationResult(String depositId, MobileMoneyInitiationResponse response) {
        PawapayDeposit deposit = serviceByDepositId(depositId);
        deposit.setInitiationAttempts(deposit.getInitiationAttempts() + 1);
        deposit.setProviderMessage(response.message());
        Map<String, Object> providerResponse = new LinkedHashMap<>();
        providerResponse.put("providerReference", response.providerReference());
        providerResponse.put("status", response.status());
        providerResponse.put("message", response.message());
        deposit.setProviderResponseJson(writeJson(providerResponse));

        if ("FAILED".equals(response.status())) {
            PawapayFailureCodes.Explanation explanation = PawapayFailureCodes.explain(response.failureReason());
            deposit.setStatus("FAILED");
            deposit.setFailureCode(explanation.code());
            deposit.setFailureReason(response.message());
            deposit.setUserMessage(explanation.userMessage());
            deposit.setRetryable(explanation.retryable());
            deposit.setFailedAt(Instant.now());
            deposit.setNextStatusCheckAt(null);
        } else if (response.unknown()) {
            deposit.setStatus("SUBMITTED_UNCONFIRMED");
            deposit.setUserMessage("Nous confirmons votre paiement auprès de l'opérateur. Ne payez pas une seconde fois.");
            deposit.setNextStatusCheckAt(Instant.now().plusSeconds(60));
        } else {
            deposit.setStatus("PROCESSING");
            deposit.setUserMessage("Validez la demande reçue sur votre téléphone avec votre code secret.");
            deposit.setNextStatusCheckAt(Instant.now().plusSeconds(60));
        }
        return repository.save(deposit);
    }

    public void markCompleted(String depositId, PawapayCallbackPayload payload) {
        PawapayDeposit deposit = repository.findByDepositId(depositId).orElse(null);
        if (deposit == null) {
            return;
        }
        deposit.setStatus("SUCCEEDED");
        deposit.setProviderMessage("COMPLETED");
        deposit.setCompletedAt(Instant.now());
        deposit.setProviderResponseJson(writeJson(payload));
        repository.save(deposit);
    }

    public void markFailed(String depositId, PawapayCallbackPayload payload, String reason) {
        PawapayDeposit deposit = repository.findByDepositId(depositId).orElse(null);
        if (deposit == null) {
            return;
        }
        deposit.setStatus("FAILED");
        deposit.setProviderMessage("FAILED");
        deposit.setFailureReason(reason);
        deposit.setFailedAt(Instant.now());
        deposit.setProviderResponseJson(writeJson(payload));
        repository.save(deposit);
    }

    public PawapayDeposit markStatusChecked(String depositId, String providerStatus) {
        return repository.findByDepositId(depositId).map(deposit -> {
            deposit.setLastStatusCheckedAt(Instant.now());
            deposit.setProviderMessage(providerStatus);
            deposit.setStatusCheckCount(deposit.getStatusCheckCount() + 1);
            return repository.save(deposit);
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public MobileMoneyDepositResponse get(String depositId) {
        return toResponse(serviceByDepositId(depositId));
    }

    @Transactional(readOnly = true)
    public List<MobileMoneyProviderOptionResponse> providers() {
        return Arrays.stream(CongoCorrespondent.values())
                .filter(CongoCorrespondent::selectable)
                .map(provider -> new MobileMoneyProviderOptionResponse(
                        provider.name(),
                        provider.getDisplayName(),
                        provider.getCountryCode(),
                        provider.getCurrency()
                ))
                .toList();
    }

    public MobileMoneyDepositResponse toResponse(PawapayDeposit deposit) {
        return new MobileMoneyDepositResponse(
                deposit.getDepositId(),
                deposit.getClientReferenceId(),
                deposit.getCustomerMessage(),
                metadata(deposit),
                new MobileMoneyDepositResponse.PayerResponse(
                        deposit.getPayerType(),
                        new MobileMoneyDepositResponse.AccountDetailsResponse(
                                deposit.getPhoneNumber(),
                                deposit.getProvider()
                        )
                ),
                deposit.getProvider(),
                deposit.getAmount(),
                deposit.getCurrency(),
                deposit.getStatus(),
                deposit.getProviderMessage(),
                deposit.getFailureReason(),
                deposit.getIntentNumber(),
                deposit.getTransactionNumber(),
                deposit.getCustomerType(),
                deposit.getCustomerCode(),
                deposit.getCallbackUrl(),
                deposit.getRequestPayloadJson(),
                deposit.getProviderResponseJson(),
                deposit.getLastStatusCheckedAt(),
                deposit.getCompletedAt(),
                deposit.getFailedAt(),
                deposit.getCreatedAt(),
                deposit.getUpdatedAt(),
                paymentMapper.toTransactionResponse(deposit.getPaymentTransaction())
        );
    }

    private PawapayDeposit serviceByDepositId(String depositId) {
        return repository.findByDepositId(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("PawaPay deposit", "depositId", depositId));
    }

    private PawapayDepositRequest toPawapayRequest(PawapayDeposit deposit, List<Map<String, Object>> metadata) {
        return new PawapayDepositRequest(
                deposit.getDepositId(),
                new PawapayDepositRequest.Payer(
                        deposit.getPayerType(),
                        new PawapayDepositRequest.AccountDetails(deposit.getPhoneNumber(), deposit.getProvider())
                ),
                deposit.getAmount().setScale(0, RoundingMode.HALF_UP).toPlainString(),
                deposit.getCurrency(),
                null,
                deposit.getClientReferenceId(),
                deposit.getCustomerMessage(),
                metadata
        );
    }

    private List<Map<String, Object>> buildMetadata(PaymentIntent intent,
                                                    PaymentTransaction transaction,
                                                    InitiateMobileMoneyDepositRequest request) {
        Map<String, Object> intentMeta = new LinkedHashMap<>();
        intentMeta.put("intentNumber", intent.getIntentNumber());
        intentMeta.put("purpose", intent.getPurpose());

        Map<String, Object> transactionMeta = new LinkedHashMap<>();
        transactionMeta.put("transactionNumber", transaction.getTransactionNumber());
        transactionMeta.put("paymentMethod", transaction.getPaymentMethod().name());

        Map<String, Object> customerMeta = new LinkedHashMap<>();
        customerMeta.put("customerType", intent.getCustomerType());
        customerMeta.put("customerCode", intent.getCustomerCode());

        Map<String, Object> sourceMeta = new LinkedHashMap<>();
        sourceMeta.put("sourceType", intent.getSourceType());
        sourceMeta.put("sourceCode", intent.getSourceCode());

        Map<String, Object> operatorMeta = new LinkedHashMap<>();
        operatorMeta.put("provider", request.correspondent().providerCode());
        operatorMeta.put("countryCode", request.correspondent().getCountryCode());
        operatorMeta.put("createdBy", clean(request.createdBy()));

        if (StringUtils.hasText(request.metadataJson())) {
            operatorMeta.put("clientMetadata", request.metadataJson().trim());
        }
        return List.of(intentMeta, transactionMeta, customerMeta, sourceMeta, operatorMeta);
    }

    private String customerMessage(PaymentIntent intent) {
        String purpose = StringUtils.hasText(intent.getPurpose()) ? intent.getPurpose().trim() : "Bokati payment";
        String normalized = purpose.replaceAll("[^A-Za-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            normalized = "Bokati payment";
        }
        if (normalized.length() < 4) {
            normalized = "Pay " + normalized;
        }
        return normalized.length() <= CUSTOMER_MESSAGE_MAX_LENGTH
                ? normalized
                : normalized.substring(0, CUSTOMER_MESSAGE_MAX_LENGTH).trim();
    }

    private List<Map<String, Object>> metadata(PawapayDeposit deposit) {
        if (!StringUtils.hasText(deposit.getMetadataJson())) {
            return List.of();
        }
        try {
            return objectMapper.readValue(deposit.getMetadataJson(), METADATA_TYPE);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    /**
     * Le MSISDN attendu par l'operateur : indicatif puis numero, chiffres seuls.
     *
     * <p>Un abonne tape son numero comme il le compose · avec ou sans indicatif, avec des espaces
     * ou un plus. L'operateur choisi dit dans quel pays on est, et c'est lui qui complete
     * l'indicatif quand il manque. Sans cela un « 06 123 45 67 » partait tel quel et l'operateur le
     * refusait, ou pire, le lisait dans un autre pays.</p>
     */
    private String msisdn(InitiateMobileMoneyDepositRequest request) {
        String dialCode = request.correspondent() == null ? null : request.correspondent().dialCode();
        boolean keepZero = request.correspondent() != null && request.correspondent().keepsTrunkZero();
        return com.sni.bokaticowork.core.utils.phone.PhoneNumbers.toE164(request.phoneNumber(), dialCode, keepZero)
                .map(e164 -> e164.substring(1))
                .orElseGet(() -> normalizePhone(request.phoneNumber()));
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[^0-9]", "");
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<Map<String, Object>> sanitizeMetadata(List<Map<String, Object>> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return List.of();
        }
        return metadata.stream()
                .map(this::sanitizeMetadataItem)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private Map<String, Object> sanitizeMetadataItem(Map<String, Object> metadataItem) {
        if (metadataItem == null || metadataItem.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        metadataItem.forEach((key, value) -> {
            if (!StringUtils.hasText(key) || value == null) {
                return;
            }
            if (value instanceof String text && !StringUtils.hasText(text)) {
                return;
            }
            sanitized.put(key, value);
        });
        return sanitized;
    }
}
