package com.sni.bokaticowork.core.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserSessionToken {

    private String accessToken;
    private String refreshToken;
}
