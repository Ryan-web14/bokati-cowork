package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOwnershipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StockInRequest {

    @NotBlank
    private String itemCode;

    @NotBlank
    private String locationCode;

    @NotNull
    @Positive
    private BigDecimal quantity;

    private Long unitCost;

    private String lotNumber;

    private LocalDate expiryDate;

    private List<String> assetSerialNumbers;

    private List<String> assetTags;

    private Boolean quarantined;

    private String quarantineReason;

    private StockOwnershipType ownershipType;

    private String ownerCode;

    private StockReferenceType referenceType;

    private String referenceCode;

    private String reason;

    private String performedBy;
}
