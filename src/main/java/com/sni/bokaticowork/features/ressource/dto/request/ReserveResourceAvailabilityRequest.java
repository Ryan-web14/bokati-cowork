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
public class ReserveResourceAvailabilityRequest {

    @NotBlank
    private String resourceCode;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private LocalDateTime endedAt;

    @NotNull
    private Integer quantity;

    public static ReserveResourceAvailabilityRequestBuilder builder() {
        return new ReserveResourceAvailabilityRequestBuilder();
    }

    public static class ReserveResourceAvailabilityRequestBuilder {
        private String resourceCode;
        private LocalDateTime startedAt;
        private LocalDateTime endedAt;
        private Integer quantity;

        public ReserveResourceAvailabilityRequestBuilder resourceCode(String resourceCode) {
            this.resourceCode = resourceCode;
            return this;
        }

        public ReserveResourceAvailabilityRequestBuilder startedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public ReserveResourceAvailabilityRequestBuilder endedAt(LocalDateTime endedAt) {
            this.endedAt = endedAt;
            return this;
        }

        public ReserveResourceAvailabilityRequestBuilder quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }

        public ReserveResourceAvailabilityRequest build() {
            return new ReserveResourceAvailabilityRequest(resourceCode, startedAt, endedAt, quantity);
        }
    }
}
