package com.sni.bokaticowork.features.client.member.model;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.enums.MemberStatus;
import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.security.admin.user.model.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "member",
        indexes = {
                @Index(name = "idx_member_customer_id", columnList = "customer_id"),
                @Index(name = "idx_member_email", columnList = "email"),
                @Index(name = "idx_member_phone", columnList = "phone"),
                @Index(name = "idx_member_status", columnList = "status")
        })
@SQLDelete(sql = "UPDATE member SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
public class Member {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column (name = "member_id", unique = true, length = 300)
    private String memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", foreignKey = @ForeignKey(name = "fk_member_customer"))
    private Customer customer;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_member_user"), unique = true,nullable = false)
    private Users user;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone", nullable = false, unique = true, length = 30)
    private String phone;

    @Column(name = "whatsapp_phone", nullable = false, length = 30)
    private String whatsappPhone;

    @Column(name = "member_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Column(name = "portal_access", nullable = false)
    @Builder.Default
    private Boolean portalAccess = false;

    @Column(name = "create_by_admin", nullable = false)
    @Builder.Default
    private Boolean createByAdmin = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PostUpdate
    public void postUpdate() {
        updatedAt = Instant.now();
    }

    @Transient
    public String getDisplayName() {
        return (firstname == null ? "" : firstname) + " " + (lastname == null ? "" : lastname);
    }



}
