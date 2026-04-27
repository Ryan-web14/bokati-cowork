package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateBookingEnabledRequest {

    @NotNull
    private Boolean bookingEnabled;
}
