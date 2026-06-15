package com.sni.bokaticowork.features.portal.billing.dto.request;

import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientInitiateMobileMoneyPaymentRequest {

    @NotBlank
    private String phoneNumber;

    @NotNull
    private CongoCorrespondent correspondent;
}
