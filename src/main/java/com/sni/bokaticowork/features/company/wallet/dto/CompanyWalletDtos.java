package com.sni.bokaticowork.features.company.wallet.dto;

import com.sni.bokaticowork.features.company.wallet.model.CompanyWalletSpend;
import com.sni.bokaticowork.features.company.wallet.model.SpendOperation;
import com.sni.bokaticowork.features.company.wallet.model.SpendingAuthorization;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Le portefeuille de l'organisation · solde, mouvements, autorisations de depense et validations.
 */
public final class CompanyWalletDtos {

    private CompanyWalletDtos() {
    }

    /** Comment l'administration a recu l'argent d'une recharge saisie a la main. */
    public enum TopUpMethod { CASH, BANK_TRANSFER }

    /**
     * Autorise un collaborateur rattache a depenser sur le portefeuille.
     *
     * @param approvalAbove au-dela, la depense attend la validation du gerant · vide, jamais
     */
    public record AuthorizationRequest(@NotBlank String memberId,
                                       @NotEmpty Set<SpendOperation> operations,
                                       @PositiveOrZero BigDecimal maxPerTransaction,
                                       @PositiveOrZero BigDecimal maxPerDay,
                                       @PositiveOrZero BigDecimal maxPerMonth,
                                       @PositiveOrZero BigDecimal approvalAbove,
                                       Instant validFrom,
                                       Instant validUntil,
                                       String currency) {
    }

    /**
     * Remplace operations, plafonds et periode · un plafond laisse vide n'en est plus un. Le statut
     * passe a {@code SUSPENDED}, {@code ACTIVE} ou {@code REVOKED} s'il est donne.
     */
    public record AuthorizationUpdateRequest(@NotEmpty Set<SpendOperation> operations,
                                             @PositiveOrZero BigDecimal maxPerTransaction,
                                             @PositiveOrZero BigDecimal maxPerDay,
                                             @PositiveOrZero BigDecimal maxPerMonth,
                                             @PositiveOrZero BigDecimal approvalAbove,
                                             Instant validFrom,
                                             Instant validUntil,
                                             SpendingAuthorization.Status status,
                                             @Size(max = 500) String reason) {
    }

    /** @param id numero court de l'autorisation */
    public record AuthorizationView(Long id,
                                    String companyCustomerCode,
                                    String walletNumber,
                                    String memberId,
                                    String memberName,
                                    Set<SpendOperation> operations,
                                    BigDecimal maxPerTransaction,
                                    BigDecimal maxPerDay,
                                    BigDecimal maxPerMonth,
                                    BigDecimal approvalAbove,
                                    Instant validFrom,
                                    Instant validUntil,
                                    SpendingAuthorization.Status status,
                                    String statusReason,
                                    boolean inForce,
                                    BigDecimal engagedToday,
                                    BigDecimal engagedThisMonth,
                                    String createdBy,
                                    Instant createdAt) {
    }

    /** @param id numero court de la depense */
    public record SpendView(Long id,
                            String memberId,
                            String memberName,
                            SpendOperation operation,
                            BigDecimal amount,
                            String currency,
                            String documentNumber,
                            CompanyWalletSpend.Status status,
                            Instant expiresAt,
                            String transactionNumber,
                            String decidedBy,
                            Instant decidedAt,
                            String decisionReason,
                            Instant createdAt) {
    }

    /** Un mouvement du portefeuille · avec le collaborateur quand c'est une depense. */
    public record EntryView(String transactionNumber,
                            WalletEntryDirection direction,
                            WalletEntryType entryType,
                            BigDecimal amount,
                            BigDecimal balanceAfter,
                            String reference,
                            String spentByMember,
                            String spentByName,
                            String documentNumber,
                            Instant createdAt) {
    }

    public record WalletView(String companyCustomerCode,
                             WalletResponse wallet,
                             List<EntryView> entries,
                             List<SpendView> pendingApprovals) {
    }

    /** Recharge saisie par l'administration · especes recues a l'accueil ou virement constate. */
    public record AdminTopUpRequest(@NotNull @Positive BigDecimal amount,
                                    String currency,
                                    @NotNull TopUpMethod method,
                                    @Size(max = 180) String reference,
                                    @NotBlank @Size(min = 5, max = 500) String reason) {
    }

    public record AdminTopUpResult(String actionNumber, String status, String walletNumber, String message) {
    }

    public record DecisionRequest(@Size(max = 500) String reason) {
    }

    public record PayRequest(String pin) {
    }

    /** Le resultat d'un reglement · execute aussitot, ou en attente du gerant. */
    public record SpendResult(SpendView spend, String message) {
    }

    /** Ce que le collaborateur voit · son autorisation et ses propres depenses, pas le solde. */
    public record MyCompanyWallet(String companyCustomerCode,
                                  String companyName,
                                  AuthorizationView authorization,
                                  List<SpendView> mySpends) {
    }
}
