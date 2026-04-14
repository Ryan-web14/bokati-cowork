package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_policy")
public class ResourcePolicy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false,unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "min_booking_duration_minutes", nullable = false)
    private Integer minBookingDurationMinutes;

    @Column(name = "max_booking_duration_minutes", nullable = false)
    private Integer maxBookingDurationMinutes;

    @Column(name = "min_booking_notice_minutes", nullable = false)
    private Integer minBookingNoticeMinutes;

    @Column(name = "cancellation_notice_minutes", nullable = false)
    private Integer cancellationNoticeMinutes;

    @Column(name = "allow_cancellation", nullable = false)
    @Builder.Default
    private Boolean allowCancellation = Boolean.FALSE;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

}
