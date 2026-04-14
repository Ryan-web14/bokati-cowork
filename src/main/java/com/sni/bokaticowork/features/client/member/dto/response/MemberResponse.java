package com.sni.bokaticowork.features.client.member.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class MemberResponse {

    private String memberId;
    private String customerId;
    private String userId;
    private String firstname;
    private String lastname;
    private String fullname;
    private String email;
    private String phone;
    private String whatsappPhone;
    private String status;
    private boolean portalAccess;
    private Instant createdAt;
    private Instant updatedAt;
}
