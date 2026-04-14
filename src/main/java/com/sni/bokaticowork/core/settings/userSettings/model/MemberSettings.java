package com.sni.bokaticowork.core.settings.userSettings.model;


import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.core.enums.BillingEntityType;
import com.sni.bokaticowork.core.enums.ReceiptPreference;
import com.sni.bokaticowork.features.client.member.model.Member;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.TimeZone;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "user_settings")
public class MemberSettings {

    @Id
    @IdGeneration
    @Column(name = "id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_member"))
    private Member member;

    @Column(name = "langage", nullable = false, length = 50 )
    private String langage;

    @Column(name = "time_zone", nullable = false, length = 100)
    private String timeZone;

    @Column(name = "show_name_on_displays", nullable = false)
    @Builder.Default
    private boolean showNameOnDisplays = false;

    @Column(name = "show_email", nullable = false)
    @Builder.Default
    private boolean showEmail = false;

    @Column(name = "show_phone", nullable = false)
    @Builder.Default
    private boolean showPhone = false;

    @Column(name = "billing_entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BillingEntityType entityType = BillingEntityType.PERSONAL;

    @Size(min = 1, max = 150)
    @Column(name = "billing_company_name")
    private String billingCompanyName;

    @Column(name = "billing_email")
    private String billingEmail;

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "wallet_auto_pop_up")
    private boolean walletAutoPopUp;

    @Column(name = "wallet_auto_topup_threshold")
    private Integer walletAutoTopupThreshold;

    @Column(name = "wallet_auto_topup_amount")
    private Integer walletAutoTopupAmount;

    @Column(name = "wallet_spending_daily_limit")
    private Integer walletSpendingDailyLimit;

    @Column(name = "wallet_spending_weekly_limit")
    private Integer walletSpendingWeeklyLimit;

    @Column(name = "wallet_receipt_preference")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReceiptPreference walletReceiptPreference = ReceiptPreference.EVERY_TRANSACTION  ;


}
