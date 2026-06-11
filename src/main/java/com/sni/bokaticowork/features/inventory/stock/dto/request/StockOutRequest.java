package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.StockOutReasonCode;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StockOutRequest {

    @NotBlank
    private String itemCode;

    @NotBlank
    private String locationCode;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    private StockOutReasonCode reasonCode;

    private StockReferenceType referenceType;

    private String referenceCode;

    private String reasonDetails;

    private Boolean allowNegativeOverride;

    @NotBlank
    private String performedBy;

    private List<String> serialNumbers;

    private String lotNumber;

    private Boolean confirmCreateLot;

    @AssertTrue(message = "referenceCode is required when referenceType is set")
    private boolean isReferenceCodeValid() {
        return referenceType == null || StringUtils.hasText(referenceCode);
    }
}
