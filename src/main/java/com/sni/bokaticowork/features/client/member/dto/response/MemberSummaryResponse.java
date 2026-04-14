package com.sni.bokaticowork.features.client.member.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MemberSummaryResponse {

    private String memberId;
    private String customerId;
    private String fullName;
    private String email;
    private String phone;
    private String whatsapp_phone;
    private String status;
    private Boolean portalAccess;
}
