package com.sni.bokaticowork.features.billing.dto.request;

import com.sni.bokaticowork.features.billing.enums.BillingDealStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Changement d'etape d'une affaire ; une affaire perdue porte son motif. */
public record DealStageRequest(@NotNull BillingDealStage stage, @Size(max = 1000) String lostReason) {
}
