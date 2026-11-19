package com.sni.bokaticowork.security.superadmin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Invitation a creer un super-administrateur, utilisable une seule fois. */
@Entity
@Table(name = "super_admin_invitation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminInvitation {

    public static final String PENDING = "PENDING";
    public static final String ISSUED = "ISSUED";
    public static final String USED = "USED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_email", nullable = false)
    private String targetEmail;

    @Column(nullable = false)
    @Builder.Default
    private String status = PENDING;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "issue_count", nullable = false)
    @Builder.Default
    private Integer issueCount = 0;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_ip")
    private String usedIp;

    @Column(name = "created_user_email")
    private String createdUserEmail;
}
