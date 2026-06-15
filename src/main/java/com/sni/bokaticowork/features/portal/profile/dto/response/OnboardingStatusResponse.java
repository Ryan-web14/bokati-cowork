package com.sni.bokaticowork.features.portal.profile.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OnboardingStatusResponse {

    private boolean emailVerified;
    private boolean profileComplete;

    private String kycStatus;
    private int kycCompletionPercent;

    private Instant gracePeriodEndsAt;
    private boolean gracePeriodActive;
    private long gracePeriodDaysRemaining;

    private boolean portalFullyActive;
    private String nextStep;

    private List<String> restrictedActions;
}
