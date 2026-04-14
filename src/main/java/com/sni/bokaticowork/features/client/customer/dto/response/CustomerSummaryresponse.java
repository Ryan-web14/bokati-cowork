package com.sni.bokaticowork.features.client.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class CustomerSummaryresponse {

    private Long id;
    private String customerId;
    private String displayName;
    private String email;
    private String phone;
    private String status;
}
