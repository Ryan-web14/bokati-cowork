package com.sni.bokaticowork.core.settings.userSettings.dto.request;

import com.sni.bokaticowork.core.enums.BillingEntityType;
import jakarta.validation.constraints.Size;

public record BillingPreferences(
        BillingEntityType billingEntityType,
        // Meme limite que la colonne : au-dela, l'enregistrement echouait en 500.
        @Size(max = 150) String billingCompanyName,
        /** Une ou plusieurs adresses separees par des virgules. */
        @Size(max = 500) @com.sni.bokaticowork.core.utils.validation.EmailList String billingEmail,
        @Size(max = 255)String billlingAddress) {
}
