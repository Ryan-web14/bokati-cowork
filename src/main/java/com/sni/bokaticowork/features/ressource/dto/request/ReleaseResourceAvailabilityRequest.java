package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReleaseResourceAvailabilityRequest {

    @NotBlank
    private String resourceCode;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private LocalDateTime endedAt;

    @NotNull
    private Integer quantity;

    /** Forme d'avant le lot R3 (sans motif ni reservation). */
    public ReleaseResourceAvailabilityRequest(String resourceCode, LocalDateTime startedAt, LocalDateTime endedAt, Integer quantity) {
        this(resourceCode, startedAt, endedAt, quantity, null, null);
    }

    /** Lot R3 : motif obligatoire d'une operation manuelle (rattrapage), journalise. */
    private String reason;

    /** Lot R3 : reservation concernee, s'il y en a une. */
    private String bookingNumber;

    public static ReleaseResourceAvailabilityRequestBuilder builder() {
        return new ReleaseResourceAvailabilityRequestBuilder();
    }

    public static class ReleaseResourceAvailabilityRequestBuilder {
        private String resourceCode;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private Integer quantity;

        public ReleaseResourceAvailabilityRequestBuilder resourceCode(String resourceCode) {
            this.resourceCode = resourceCode;
            return this;
        }

        public ReleaseResourceAvailabilityRequestBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ReleaseResourceAvailabilityRequestBuilder endedAt(LocalDateTime endedAt) {
            this.endedAt = endedAt;
            return this;
        }

        public ReleaseResourceAvailabilityRequestBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public ReleaseResourceAvailabilityRequest build() {
            return new ReleaseResourceAvailabilityRequest(resourceCode, startedAt, endedAt, quantity);
        }
    }
}
