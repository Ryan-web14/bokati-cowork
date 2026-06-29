package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateFolderRequest {

    @NotBlank
    private String name;

    private String description;

    private String parentCode;

    private DocumentSpace space;

    private DocumentOwnerType ownerType;

    private String ownerCode;

    private String color;

    private String icon;
}
