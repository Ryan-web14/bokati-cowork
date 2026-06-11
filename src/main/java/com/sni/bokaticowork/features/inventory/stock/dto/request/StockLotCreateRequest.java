package com.sni.bokaticowork.features.inventory.stock.dto.request;

import com.sni.bokaticowork.features.inventory.stock.enums.StockOwnershipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockLotCreateRequest {

    @NotBlank
    private String locationCode;

    @NotBlank
    private String lotNumber;

    private LocalDate expiryDate;

    @NotNull
    @Positive
    private BigDecimal initialQuantity;

    private StockOwnershipType ownershipType;

    private String ownerCode;
}
