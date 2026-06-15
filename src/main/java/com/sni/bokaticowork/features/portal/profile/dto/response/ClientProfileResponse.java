package com.sni.bokaticowork.features.portal.profile.dto.response;

import java.time.Instant;

public record ClientProfileResponse(
        String memberId,
        String firstname,
        String lastname,
        String fullName,
        String email,
        String phone,
        String whatsappPhone,
        String status,
        String avatarUrl,
        Instant portalActivatedAt,
        Instant createdAt,
        Instant updatedAt
) {}
