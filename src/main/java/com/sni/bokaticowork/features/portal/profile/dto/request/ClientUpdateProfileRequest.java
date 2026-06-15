package com.sni.bokaticowork.features.portal.profile.dto.request;

import jakarta.validation.constraints.Size;

public record ClientUpdateProfileRequest(
        @Size(max = 350) String firstname,
        @Size(max = 350) String lastname,
        @Size(max = 30) String phone,
        @Size(max = 30) String whatsappPhone
) {}
