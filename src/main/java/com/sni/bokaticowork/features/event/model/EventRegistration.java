package com.sni.bokaticowork.features.event.model;

import com.sni.bokaticowork.features.event.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "event_registration", indexes = {
        @Index(name = "idx_event_registration_status", columnList = "status"),
        @Index(name = "idx_event_registration_email", columnList = "email"),
        @Index(name = "idx_event_registration_event", columnList = "event_id")
})
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_number", unique = true, length = 80)
    private String registrationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "firstname", nullable = false, length = 180)
    private String firstname;

    @Column(name = "lastname", nullable = false, length = 180)
    private String lastname;

    @Column(name = "email", nullable = false, length = 250)
    private String email;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "whatsapp_phone", length = 30)
    private String whatsappPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RegistrationStatus status;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "validated_by")
    private Long validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "member_id", length = 60)
    private String memberId;

    @Column(name = "customer_id", length = 60)
    private String customerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = updatedAt = Instant.now();
        if (registrationNumber == null) {
            registrationNumber = "REG-" + System.currentTimeMillis();
        }
        if (status == null) {
            status = RegistrationStatus.PENDING_VALIDATION;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
