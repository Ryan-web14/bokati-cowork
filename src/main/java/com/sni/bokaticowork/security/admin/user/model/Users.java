package com.sni.bokaticowork.security.admin.user.model;


import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.security.admin.role.model.RoleUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Users {

    @Id
    @IdGeneration
    @Column(name = "id")
    private long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "firstname", length = 255)
    private String firstname;

    @Column(name = "lastname", length = 255)
    private String lastname;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "deleted")
    private boolean deleted = Boolean.FALSE;

    @Column(name = "is_account_expired")
    private Boolean isAccountExpired;

    @Column(name = "is_account_locked")
    private Boolean isAccountLocked;

    @Column(name = "is_account_enabled")
    private Boolean isAccountEnabled;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    @Column(name = "pending_email", length = 250)
    private String pendingEmail;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<RoleUser> roleUsers;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
}
