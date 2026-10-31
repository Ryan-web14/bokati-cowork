package com.sni.bokaticowork.features.portal.booking.dto.request;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClientCheckAvailabilityRequest {

    @NotBlank
    private String resourceCode;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private LocalDateTime endedAt;

    @Min(1)
    private Integer quantity = 1;

    /** Vide : le creneau seul. SUBSCRIPTION ou PASS : verifie aussi qu'un droit couvre la reservation. */
    private BookingPaymentMode paymentMode;

    /** L'organisation pour laquelle je reserve · {@code NONE} pour aucune. */
    private String billingCompanyCode;
}
