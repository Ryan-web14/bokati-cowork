package com.sni.bokaticowork.features.client.member.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycGracePeriodRequest {

    @NotNull
    @Min(value = 1, message = "Grace period must be at least 1 day")
    @Max(value = 365, message = "Grace period cannot exceed 365 days")
    private Integer gracePeriodDays;
}
