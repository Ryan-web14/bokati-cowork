package com.sni.bokaticowork.features.portal.subscription.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientCancelSubscriptionRequest {

    @Size(max = 500)
    private String reason;
}
