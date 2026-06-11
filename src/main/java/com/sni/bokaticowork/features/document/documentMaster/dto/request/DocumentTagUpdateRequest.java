package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentTagUpdateRequest {

    @Size(max = 150)
    private String label;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code like #3B82F6")
    private String color;
}
