package com.sni.bokaticowork.features.payment.dto.request;

import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitiateMobileMoneyDepositRequest(
        String intentNumber,
        @NotBlank String phoneNumber,
        @NotNull CongoCorrespondent correspondent,
        BigDecimal amount,
        String createdBy,
        String metadataJson
) {}
