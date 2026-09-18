package com.sni.bokaticowork.features.payment.security.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.security.enums.PinRequirement;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import com.sni.bokaticowork.features.payment.security.enums.WalletSecurityScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Quand le code secret est exige, et ce qui se passe quand il est mal saisi.
 *
 * <p>Le code n'est pas impose a tous. L'exiger pour consulter un solde ou regler une facture deja
 * connue ajoute une friction que rien ne justifie, et pousse les titulaires vers des codes triviaux
 * notes quelque part.</p>
 *
 * <p>Une exception, et elle n'en souffre aucune : <b>un transfert vers un autre abonne exige
 * toujours le code</b>. C'est la seule operation qui fait sortir de l'argent vers quelqu'un
 * d'autre, sans facture en face et qu'aucune annulation de prestation ne rattrapera. C'est aussi,
 * pour cette raison exacte, celle qui interesse un compte vole.</p>
 *
 * <p>{@code TRANSFER} est donc impose dans {@link #requiredOperations()} quoi qu'on ait saisi : la
 * regle ne tient pas par la discipline de celui qui configure.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_security_policy")
public class WalletSecurityPolicy {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 40)
    @Builder.Default
    private WalletSecurityScope scope = WalletSecurityScope.GLOBAL;

    @Column(name = "scope_code", length = 120)
    private String scopeCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "pin_requirement", nullable = false, length = 40)
    @Builder.Default
    private PinRequirement pinRequirement = PinRequirement.OPTIONAL;

    @Column(name = "pin_required_operations", nullable = false, columnDefinition = "text")
    @Builder.Default
    private String pinRequiredOperations = WalletOperationType.TRANSFER.name();

    @Column(name = "pin_length", nullable = false)
    @Builder.Default
    private Integer pinLength = 4;

    @Column(name = "pin_expiry_days")
    private Integer pinExpiryDays;

    @Column(name = "max_failed_attempts", nullable = false)
    @Builder.Default
    private Integer maxFailedAttempts = 5;

    @Column(name = "lockout_minutes", nullable = false)
    @Builder.Default
    private Integer lockoutMinutes = 15;

    /** Chaque verrouillage double la temporisation · un essai automatise s'epuise vite. */
    @Column(name = "lockout_escalation", nullable = false)
    @Builder.Default
    private Boolean lockoutEscalation = Boolean.TRUE;

    /** Montant au-dela duquel un second canal s'ajoute au code. */
    @Column(name = "otp_threshold_amount", precision = 19, scale = 4)
    private BigDecimal otpThresholdAmount;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Operations exigeant le code, {@code TRANSFER} compris quoi qu'il arrive.
     *
     * <p>L'imposer ici plutot qu'a la saisie ferme la porte a toutes les facons de le retirer :
     * une modification directe en base, un import de configuration, un appel d'API qui oublierait
     * de le remettre.</p>
     */
    public Set<WalletOperationType> requiredOperations() {
        Set<WalletOperationType> operations = new LinkedHashSet<>();
        operations.add(WalletOperationType.TRANSFER);
        if (pinRequiredOperations == null || pinRequiredOperations.isBlank()) {
            return operations;
        }
        Arrays.stream(pinRequiredOperations.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .forEach(value -> {
                    try {
                        operations.add(WalletOperationType.valueOf(value));
                    } catch (IllegalArgumentException ignored) {
                        // Une operation inconnue est ignoree · une politique illisible ne doit pas
                        // empecher le transfert d'exiger son code.
                    }
                });
        return operations;
    }

    public boolean requiresPinFor(WalletOperationType operation) {
        if (pinRequirement == PinRequirement.REQUIRED) {
            return true;
        }
        if (pinRequirement == PinRequirement.DISABLED) {
            // Meme desactive, le transfert garde son code · c'est le sens du mot « impose ».
            return operation == WalletOperationType.TRANSFER;
        }
        return requiredOperations().contains(operation);
    }

    /** Rang de precision, du plus precis au plus general, pour departager deux politiques. */
    public int specificity() {
        return switch (scope) {
            case WALLET -> 0;
            case OWNER_TYPE -> 1;
            case GLOBAL -> 2;
        };
    }

    public boolean activeAt(Instant moment) {
        return (effectiveFrom == null || !moment.isBefore(effectiveFrom))
                && (effectiveTo == null || !moment.isAfter(effectiveTo));
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
