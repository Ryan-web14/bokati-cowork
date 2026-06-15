package com.sni.bokaticowork.features.portal.session.dto.response;

import java.time.Instant;

public record ClientSessionResponse(
        Long id,
        Instant expiresAt,
        boolean active
) {}
