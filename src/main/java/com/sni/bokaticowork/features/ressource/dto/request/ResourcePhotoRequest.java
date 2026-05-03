package com.sni.bokaticowork.features.ressource.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourcePhotoRequest {
    @NotBlank
    private String documentCode;
    private String caption;
    private Boolean cover;
    private Integer displayOrder;
    private Boolean active;
}
