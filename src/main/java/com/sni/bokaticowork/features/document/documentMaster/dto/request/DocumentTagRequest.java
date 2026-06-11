package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentTagRequest {

    @NotBlank
    @Size(max = 80)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Code must be lowercase alphanumeric with hyphens")
    private String code;

    @NotBlank
    @Size(max = 150)
    private String label;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code like #3B82F6")
    private String color;

    private DocumentSpace space;
}
