package com.sni.bokaticowork.features.subscription.subscription.dto.request;

import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateSubscriptionRequest(
        @NotBlank String planCode,
        String planVersionId,
        BillingCycle billingCycle,
        @NotNull SubscriberType subscriberType,
        @NotBlank String subscriberCode,
        @NotNull LocalDate startDate,
        Boolean autoRenew,
        String metadataJson,
        Boolean autoActivate,
        /** Codes de reduction saisis par le souscripteur. Nul ou vide s'il n'en presente aucun. */
        List<String> couponCodes
) {

    /** Souscription sans code de reduction, forme la plus courante. */
    public CreateSubscriptionRequest(String planCode, String planVersionId, BillingCycle billingCycle,
                                     SubscriberType subscriberType, String subscriberCode,
                                     LocalDate startDate, Boolean autoRenew, String metadataJson,
                                     Boolean autoActivate) {
        this(planCode, planVersionId, billingCycle, subscriberType, subscriberCode, startDate,
                autoRenew, metadataJson, autoActivate, List.of());
    }

    public List<String> couponCodesOrEmpty() {
        return couponCodes == null ? List.of() : couponCodes;
    }
}
