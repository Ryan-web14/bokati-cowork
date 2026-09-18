package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassValidityRuleType;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassValidityRule;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassCredentialRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassUsageRepository;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassValidityRuleRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Repond oui ou non, et dit pourquoi.
 *
 * <p><b>Il n'y a qu'une seule facon de valider un pass.</b> Le guichet, la reservation en ligne, la
 * borne et l'API appellent ce service. Dupliquer la regle entre trois modules produirait trois
 * comportements divergents, et c'est toujours celui qu'on n'a pas teste qui laisse passer.</p>
 *
 * <p>Le canal ne change jamais la decision. Il est conserve sur l'usage, pour savoir d'ou viennent
 * les entrees et pour enqueter sur un refus conteste, mais un pass refuse a la borne doit etre
 * refuse au guichet.</p>
 *
 * <p>Ce service <b>ne consomme rien</b>. Il constate. La consommation appartient a
 * {@link PassUsageService}, et les separer permet d'afficher « ce pass vous ouvre l'acces » avant
 * que quiconque n'ait franchi la porte.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassValidationService {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");

    private final PassRepository passRepository;
    private final PassCredentialRepository credentialRepository;
    private final PassValidityRuleRepository validityRuleRepository;
    private final PassBeneficiaryRepository beneficiaryRepository;
    private final PassUsageRepository usageRepository;

    /**
     * Ce qu'on presente et dans quelles circonstances.
     *
     * @param bearerCode celui qui se presente · nul lorsque personne ne s'identifie, ce qui est le
     *                   cas d'une borne ou seul le pass est scanne
     */
    public record ValidationRequest(
            String passNumber,
            String credentialValue,
            String bearerType,
            String bearerCode,
            String locationCode,
            String resourceTypeCode,
            Instant at
    ) {

        public Instant atOrNow() {
            return at == null ? Instant.now() : at;
        }
    }

    /**
     * @param reason motif redige en francais · un refus muet renvoie la personne vers l'accueil, ou
     *               personne ne saura davantage lui repondre
     */
    public record ValidationResult(
            boolean valid,
            String reason,
            Pass pass,
            PassBeneficiary beneficiary,
            Integer remainingUses
    ) {

        static ValidationResult refused(String reason) {
            return new ValidationResult(false, reason, null, null, null);
        }

        static ValidationResult refused(String reason, Pass pass) {
            return new ValidationResult(false, reason, pass, null, null);
        }
    }

    @Transactional(readOnly = true)
    public ValidationResult validate(ValidationRequest request) {
        Optional<Pass> found = resolvePass(request);
        if (found.isEmpty()) {
            return ValidationResult.refused("Ce pass est introuvable");
        }
        Pass pass = found.get();
        Instant now = request.atOrNow();

        String statusRefusal = refusalForStatus(pass);
        if (statusRefusal != null) {
            return ValidationResult.refused(statusRefusal, pass);
        }
        if (pass.getValidFrom() != null && now.isBefore(pass.getValidFrom())) {
            return ValidationResult.refused("Ce pass n'est pas encore valable", pass);
        }
        if (pass.getValidUntil() != null && now.isAfter(pass.getValidUntil())) {
            return ValidationResult.refused("Ce pass a expiré", pass);
        }

        Integer remaining = remainingUses(pass);
        if (remaining != null && remaining <= 0) {
            return ValidationResult.refused("Ce pass est entièrement consommé", pass);
        }

        PassBeneficiary beneficiary = null;
        if (StringUtils.hasText(request.bearerCode())) {
            BearerCheck bearer = checkBearer(pass, request, now);
            if (bearer.refusal() != null) {
                return ValidationResult.refused(bearer.refusal(), pass);
            }
            beneficiary = bearer.beneficiary();
        }

        String ruleRefusal = checkRules(pass, request, now);
        if (ruleRefusal != null) {
            return ValidationResult.refused(ruleRefusal, pass);
        }

        return new ValidationResult(true, null, pass, beneficiary, remaining);
    }

    // -----------------------------------------------------------------------------------------

    private Optional<Pass> resolvePass(ValidationRequest request) {
        if (StringUtils.hasText(request.credentialValue())) {
            return credentialRepository.findActiveByValue(request.credentialValue().trim())
                    .filter(credential -> credential.usableAt(request.atOrNow()))
                    .map(credential -> credential.getPass());
        }
        if (StringUtils.hasText(request.passNumber())) {
            return passRepository.findByPassNumber(request.passNumber().trim());
        }
        return Optional.empty();
    }

    private String refusalForStatus(Pass pass) {
        return switch (pass.getStatus()) {
            case ACTIVE, PARTIALLY_USED -> null;
            case PENDING_ACTIVATION -> "Ce pass n'est pas encore activé";
            case CONSUMED -> "Ce pass est entièrement consommé";
            case EXPIRED -> "Ce pass a expiré";
            case SUSPENDED -> "Ce pass est suspendu";
            case CANCELLED -> "Ce pass a été annulé";
            case PAST_DUE -> "Ce pass est en attente de règlement";
            case DRAFT -> "Ce pass n'est pas utilisable";
        };
    }

    /**
     * Qui a le droit de s'en servir.
     *
     * <p>Sans liste de beneficiaires, seul le titulaire l'utilise. Avec une liste, le titulaire et
     * les personnes designees. Le quota individuel se verifie ici, et non au decompte : annoncer
     * l'acces puis le refuser a la porte serait pire que de refuser tout de suite.</p>
     */
    private BearerCheck checkBearer(Pass pass, ValidationRequest request, Instant now) {
        boolean isHolder = pass.getOwnerCode() != null
                && pass.getOwnerCode().equalsIgnoreCase(request.bearerCode());

        Optional<PassBeneficiary> listed = beneficiaryRepository.find(
                pass.getId(), request.bearerType(), request.bearerCode());

        if (listed.isPresent()) {
            PassBeneficiary beneficiary = listed.get();
            if (!beneficiary.usableAt(now)) {
                return new BearerCheck("Cette personne n'est plus autorisée à utiliser ce pass", null);
            }
            Integer remaining = beneficiary.remainingForBeneficiary();
            if (remaining != null && remaining <= 0) {
                return new BearerCheck("Cette personne a atteint son quota sur ce pass", null);
            }
            return new BearerCheck(null, beneficiary);
        }

        if (isHolder) {
            return new BearerCheck(null, null);
        }
        if (!Boolean.TRUE.equals(pass.getShareable())) {
            return new BearerCheck("Ce pass n'est utilisable que par son titulaire", null);
        }
        return new BearerCheck("Cette personne n'est pas autorisée à utiliser ce pass", null);
    }

    /**
     * Regles de validite fines.
     *
     * <p>Une interdiction l'emporte toujours sur une autorisation. « Valable en semaine » et
     * « interdit les jours feries » doivent cohabiter, et le jour ferie doit gagner · c'est
     * l'exception qui a ete posee en dernier et qui a un motif.</p>
     */
    private String checkRules(Pass pass, ValidationRequest request, Instant now) {
        List<PassValidityRule> rules = validityRuleRepository.findApplicable(
                pass.getId(), pass.getPassVersion() == null ? null : pass.getPassVersion().getId());
        if (rules.isEmpty()) {
            return null;
        }

        for (PassValidityRule rule : rules) {
            String refusal = check(rule, pass, request, now);
            if (refusal != null) {
                return refusal;
            }
        }
        return null;
    }

    private String check(PassValidityRule rule, Pass pass, ValidationRequest request, Instant now) {
        boolean allow = !Boolean.FALSE.equals(rule.getAllowRule());
        return switch (rule.getRuleType()) {
            case DAY_OF_WEEK -> matchesDay(rule, now) == allow ? null
                    : "Ce pass n'est pas utilisable ce jour de la semaine";
            case TIME_RANGE -> matchesTime(rule, now) == allow ? null
                    : "Ce pass n'est pas utilisable à cette heure";
            case LOCATION -> matches(rule, request.locationCode()) == allow ? null
                    : "Ce pass n'est pas utilisable sur ce site";
            case RESOURCE_TYPE -> matches(rule, request.resourceTypeCode()) == allow ? null
                    : "Ce pass ne donne pas accès à ce type de ressource";
            // Une date bloquee est toujours une interdiction · allowRule n'a pas de sens ici.
            case BLACKOUT_DATE -> matchesDate(rule, now)
                    ? "Ce pass n'est pas utilisable à cette date" : null;
            case MAX_PER_DAY -> withinDailyCap(rule, pass, now) ? null
                    : "Ce pass a déjà été utilisé le nombre de fois autorisé aujourd'hui";
            case MAX_CONCURRENT -> withinConcurrentCap(rule, pass) ? null
                    : "Ce pass est déjà en cours d'utilisation";
        };
    }

    private boolean matchesDay(PassValidityRule rule, Instant now) {
        DayOfWeek day = now.atZone(APP_ZONE).getDayOfWeek();
        return values(rule).stream().anyMatch(value -> value.equalsIgnoreCase(day.name()));
    }

    private boolean matchesTime(PassValidityRule rule, Instant now) {
        List<String> bounds = rule.values();
        if (bounds.size() != 2) {
            // Une regle horaire illisible n'autorise rien : refuser se corrige, laisser passer non.
            return false;
        }
        try {
            LocalTime moment = now.atZone(APP_ZONE).toLocalTime();
            LocalTime from = LocalTime.parse(bounds.get(0));
            LocalTime to = LocalTime.parse(bounds.get(1));
            if (from.isAfter(to)) {
                return !moment.isBefore(from) || !moment.isAfter(to);
            }
            return !moment.isBefore(from) && !moment.isAfter(to);
        } catch (Exception ex) {
            log.debug("Regle horaire {} illisible sur le pass", rule.getId());
            return false;
        }
    }

    private boolean matchesDate(PassValidityRule rule, Instant now) {
        LocalDate today = now.atZone(APP_ZONE).toLocalDate();
        return values(rule).stream().anyMatch(value -> {
            try {
                return LocalDate.parse(value).equals(today);
            } catch (Exception ex) {
                return false;
            }
        });
    }

    private boolean matches(PassValidityRule rule, String observed) {
        if (!StringUtils.hasText(observed)) {
            // Rien a comparer · la regle ne peut ni autoriser ni interdire, elle ne s'applique pas.
            return !Boolean.FALSE.equals(rule.getAllowRule());
        }
        return values(rule).stream().anyMatch(value -> value.equalsIgnoreCase(observed.trim()));
    }

    private boolean withinDailyCap(PassValidityRule rule, Pass pass, Instant now) {
        int cap = intValue(rule);
        if (cap <= 0) {
            return true;
        }
        LocalDate today = now.atZone(APP_ZONE).toLocalDate();
        Instant dayStart = today.atStartOfDay(APP_ZONE).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        return usageRepository.countOnDay(pass.getId(), dayStart, dayEnd) < cap;
    }

    private boolean withinConcurrentCap(PassValidityRule rule, Pass pass) {
        int cap = intValue(rule);
        if (cap <= 0) {
            return true;
        }
        return usageRepository.countInFlight(pass.getId()) < cap;
    }

    private List<String> values(PassValidityRule rule) {
        List<String> list = rule.values();
        return list.isEmpty() && StringUtils.hasText(rule.getValue())
                ? List.of(rule.getValue().trim().toUpperCase(Locale.ROOT))
                : list;
    }

    private int intValue(PassValidityRule rule) {
        try {
            return rule.getValue() == null ? 0 : Integer.parseInt(rule.getValue().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    Integer remainingUses(Pass pass) {
        if (pass.getMaxUses() == null) {
            return null;
        }
        return Math.max(0, pass.getMaxUses() - (pass.getUsedCount() == null ? 0 : pass.getUsedCount()));
    }

    private record BearerCheck(String refusal, PassBeneficiary beneficiary) {
    }
}
