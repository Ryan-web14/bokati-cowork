package com.sni.bokaticowork.features.portal.support.dto.request;

import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientCreateTicketRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        TicketPriority priority,
        TicketCategory category,
        String relatedType,
        String relatedCode
) {}
