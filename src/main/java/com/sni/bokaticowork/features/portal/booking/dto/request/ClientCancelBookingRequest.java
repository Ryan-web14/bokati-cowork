package com.sni.bokaticowork.features.portal.booking.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientCancelBookingRequest {

    @Size(max = 500)
    private String reason;
}
