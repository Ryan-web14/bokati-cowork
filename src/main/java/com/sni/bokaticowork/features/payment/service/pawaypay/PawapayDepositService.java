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
        String phoneNumber = normalizePhone(request.phoneNumber());
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

    public PawapayDeposit markInitiationResult(String depositId, MobileMoneyInitiationResponse response) {
        PawapayDeposit deposit = serviceByDepositId(depositId);
        deposit.setStatus(response.status());
        deposit.setProviderMessage(response.message());
        Map<String, Object> providerResponse = new LinkedHashMap<>();
        providerResponse.put("providerReference", response.providerReference());
        providerResponse.put("status", response.status());
        providerResponse.put("message", response.message());
        deposit.setProviderResponseJson(writeJson(providerResponse));
        if ("FAILED".equals(response.status())) {
            deposit.setFailureReason(response.message());
            deposit.setFailedAt(Instant.now());
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

    public void markStatusChecked(String depositId, String providerStatus) {
        repository.findByDepositId(depositId).ifPresent(deposit -> {
            deposit.setLastStatusCheckedAt(Instant.now());
            deposit.setProviderMessage(providerStatus);
            repository.save(deposit);
        });
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
                deposit.getCallbackUrl(),
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

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[^\\d]", "");
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
