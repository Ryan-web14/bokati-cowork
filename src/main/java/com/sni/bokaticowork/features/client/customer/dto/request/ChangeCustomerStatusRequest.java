package com.sni.bokaticowork.features.client.customer.dto.request;

import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeCustomerStatusRequest {

    @NotNull
    private CustomerStatus status;

}
