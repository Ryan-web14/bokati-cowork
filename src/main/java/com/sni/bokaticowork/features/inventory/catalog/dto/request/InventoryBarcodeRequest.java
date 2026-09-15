package com.sni.bokaticowork.features.inventory.catalog.dto.request;

import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryBarcodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryBarcodeRequest {

    @NotNull
    private InventoryBarcodeType barcodeType;

    @NotBlank
    private String barcodeValue;

    /** Unite representee, lorsque le code designe un conditionnement. */
    private String unitCode;

    /** Nombre d'unites de base representees par un scan. Un par defaut. */
    @Positive
    private BigDecimal quantity;

    /** Demande a faire de ce code le code principal de l'article. */
    private Boolean primaryCode;

    private Boolean active;
}
