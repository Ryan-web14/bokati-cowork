package com.sni.bokaticowork.features.accounting.service.support;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Parametres de la comptabilite, modifiables a chaud depuis l'ecran des parametres (domaine
 * Comptabilite, reserve au super-administrateur), avec historique. Aucune valeur metier n'est
 * codee en dur : le code ne porte que les valeurs initiales (plan, section 20).
 */
@Component
@RequiredArgsConstructor
public class AccountingSettings {

    private final PlatformSettings settings;

    public boolean enabled() {
        return bool("accounting.enabled", false);
    }

    public boolean analyticEnabled() {
        return bool("accounting.analytic.enabled", false);
    }

    public UnbalancedPolicy unbalancedPolicy() {
        String value = settings.text("accounting.unbalanced-entry.policy", "HOLD");
        try {
            return UnbalancedPolicy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return UnbalancedPolicy.HOLD;
        }
    }

    public String suspenseAccount() {
        return settings.text("accounting.unbalanced-entry.suspense-account", "4718").trim();
    }

    /** Difference maximale passee en compte d'attente ; nul : sans limite. */
    public BigDecimal suspenseMaxDifference() {
        long value = settings.number("accounting.unbalanced-entry.suspense-max-difference", 0);
        return value <= 0 ? null : BigDecimal.valueOf(value);
    }

    public boolean unbalancedBlocksPeriodClose() {
        return bool("accounting.unbalanced-entry.blocks-period-close", true);
    }

    public boolean strictChronology() {
        return !"PERIOD".equalsIgnoreCase(settings.text("accounting.numbering.chronology", "STRICT").trim());
    }

    public int numberingWidth() {
        return (int) Math.max(3, Math.min(10, settings.number("accounting.numbering.width", 6)));
    }

    public BigDecimal letteringDifferenceThreshold() {
        return BigDecimal.valueOf(Math.max(0, settings.number("accounting.lettering.difference-threshold", 500)));
    }

    public String letteringLossAccount() {
        return settings.text("accounting.lettering.loss-account", "658").trim();
    }

    public String letteringGainAccount() {
        return settings.text("accounting.lettering.gain-account", "758").trim();
    }

    public boolean accountCreationNeedsApproval() {
        return "AFTER_APPROVAL".equalsIgnoreCase(settings.text("accounting.account.creation-mode", "IMMEDIATE").trim());
    }

    /** Montant d'une piece manuelle au-dela duquel un autre comptable la valide ; nul : jamais. */
    public BigDecimal secondValidationThreshold() {
        long value = settings.number("accounting.manual-entry.second-validation-threshold", 0);
        return value <= 0 ? null : BigDecimal.valueOf(value);
    }

    public List<String> notifyEmails() {
        String value = settings.text("accounting.notify-emails", "");
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).toList();
    }

    /** Date de bascule : les evenements dates avant ne sont pas comptabilises (sauf reprise). */
    public java.time.LocalDate startDate() {
        String value = settings.text("accounting.start-date", "2026-01-01");
        try {
            return java.time.LocalDate.parse(value.trim());
        } catch (RuntimeException exception) {
            return java.time.LocalDate.of(2026, 1, 1);
        }
    }

    /** Famille d'evenements comptabilisee (interrupteur par famille, lot F2). */
    public boolean familyEnabled(String family) {
        if (!StringUtils.hasText(family)) {
            return true;
        }
        String disabled = settings.text("accounting.posting.disabled-families", "");
        if (!StringUtils.hasText(disabled)) {
            return true;
        }
        return Arrays.stream(disabled.split(",")).map(String::trim).noneMatch(value -> value.equalsIgnoreCase(family.trim()));
    }

    /** Reglage texte de la comptabilite (comptes parametres). */
    public String text(String key, String fallback) {
        String value = settings.text(key, fallback);
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /**
     * Reprise de l'historique : les faits dates avant ce jour produisent des brouillards, valides
     * en bloc apres controle. Vide : pas de reprise.
     */
    public java.time.LocalDate replayUntil() {
        String value = settings.text("accounting.replay-until", "");
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.trim());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public String disabledFamilies() {
        return settings.text("accounting.posting.disabled-families", "");
    }

    public int postingBatchSize() {
        return (int) Math.max(1, Math.min(500, settings.number("accounting.posting.batch-size", 50)));
    }

    private boolean bool(String key, boolean fallback) {
        String value = settings.text(key, String.valueOf(fallback));
        return "true".equalsIgnoreCase(value == null ? null : value.trim());
    }
}
