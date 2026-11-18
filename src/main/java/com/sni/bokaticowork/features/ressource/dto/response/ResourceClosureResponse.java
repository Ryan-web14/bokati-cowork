package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourceClosureResponse {

    private Long id;
    private String resourceCode;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Boolean active;
    /** Lot R2 : motif de la fermeture (avant : jamais rendu). */
    private String reason;

    /** Lot R5a. */
    private String kind;

    private String recurrence;

    private java.time.LocalDate recurrenceUntil;
}
