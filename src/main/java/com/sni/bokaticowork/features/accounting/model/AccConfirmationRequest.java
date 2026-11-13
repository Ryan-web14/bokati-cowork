package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Confirmation de solde d'un tiers (circularisation), du courrier a la reponse (lot F12). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_confirmation_request")
public class AccConfirmationRequest {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "third_party_code")
    private String thirdPartyCode;

    @Column(name = "balance_date")
    private LocalDate balanceDate;

    @Column(name = "balance")
    private BigDecimal balance;

    @Column(name = "open_items")
    private String openItems;

    @Builder.Default
    @Column(name = "status")
    private String status = "PREPARED";

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "reminded_at")
    private Instant remindedAt;

    @Column(name = "reply_channel")
    private String replyChannel;

    @Column(name = "replied_balance")
    private BigDecimal repliedBalance;

    @Column(name = "reply_detail")
    private String replyDetail;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Column(name = "difference")
    private BigDecimal difference;

    @Column(name = "reconciled_by")
    private String reconciledBy;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    @Column(name = "created_by")
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    /** Canal du dernier envoi : EMAIL, WHATSAPP ou PRINT (complements F12). */
    @Column(name = "sent_channel")
    private String sentChannel;

    /** Adresse ou numero du dernier envoi. */
    @Column(name = "recipient")
    private String recipient;
}
