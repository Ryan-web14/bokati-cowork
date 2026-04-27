package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDisplayOrderRequest {

    @NotNull
    private Integer displayOrder;
}
