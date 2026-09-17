package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.LotBlockReasonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LotBlockRequest {

    @NotNull
    private LotBlockReasonType reasonType;

    @NotBlank
    private String reason;

    private String blockedBy;
}
