package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourcePolicyResponse {

    private String code;
    private String name;
    private String description;
    private Integer minBookingDurationMinutes;
    private Integer maxBookingDurationMinutes;
    private Integer minBookingNoticeMinutes;
    private Integer cancellationNoticeMinutes;
    private Boolean allowCancellation;
    /** L'equipe peut-elle reserver hors preavis et sur un debut passe ? Vrai par defaut. */
    private Boolean staffOverrideAllowed;
    private Boolean active;
}
