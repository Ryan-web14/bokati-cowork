package com.sni.bokaticowork.features.ressource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ResourcePhotoResponse {
    private Long id;
    private String resourceCode;
    private String documentCode;
    private String caption;
    private Boolean cover;
    private Integer displayOrder;
    private Boolean active;
}
