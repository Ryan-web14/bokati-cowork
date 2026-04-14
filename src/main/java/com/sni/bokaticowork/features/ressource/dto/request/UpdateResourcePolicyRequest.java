package com.sni.bokaticowork.features.ressource.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UpdateResourcePolicyRequest {

    private String name;
    private String description;
    private Integer minBookingDurationMinutes;
    private Integer maxBookingDurationMinutes;
    private Integer minBookingNoticeMinutes;
    private Integer cancellationNoticeMinutes;
    private Boolean allowCancellation;
    private Boolean active;
}
