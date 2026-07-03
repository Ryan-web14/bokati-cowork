package com.sni.bokaticowork.features.payment.provider.pawaypay;

import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositRequest;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayDepositStatusResponse;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundRequest;
import com.sni.bokaticowork.features.payment.provider.pawaypay.dto.PawapayRefundResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

@Slf4j
public class PawapayClient {

    private final RestClient restClient;

    PawapayClient(PawapayProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public PawapayDepositResponse initiateDeposit(PawapayDepositRequest request) {
        log.debug("Initiating PawaPay deposit: depositId={}, provider={}",
                request.depositId(), request.payer().accountDetails().provider());
        return restClient.post()
                .uri("/v2/deposits")
                .body(request)
                .retrieve()
                .body(PawapayDepositResponse.class);
    }

    public PawapayDepositStatusResponse getDepositStatus(String depositId) {
        log.debug("Checking PawaPay deposit status: depositId={}", depositId);
        return restClient.get()
                .uri("/v2/deposits/{depositId}", depositId)
                .retrieve()
                .body(PawapayDepositStatusResponse.class);
    }

    public PawapayRefundResponse initiateRefund(PawapayRefundRequest request) {
        log.debug("Initiating PawaPay refund: refundId={}, depositId={}", request.refundId(), request.depositId());
        return restClient.post()
                .uri("/v2/refunds")
                .body(request)
                .retrieve()
                .body(PawapayRefundResponse.class);
    }
}
