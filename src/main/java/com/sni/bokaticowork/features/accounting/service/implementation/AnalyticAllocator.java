package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ventilation analytique des lignes d'ecriture (lot F9, analytique-et-budget section 1.2).
 *
 * <p>Pour chaque axe actif, la section d'une ligne de charge (classe 6) ou de produit (classe 7),
 * ou d'un compte marque analytique, vient, par ordre de priorite : de la saisie ou du fait metier
 * ({@code analytic} de la ligne), d'une regle (compte, journal, type de fait, tiers), du compte
 * (correspondance section et comptes), de la section par defaut de l'axe. Une cle de repartition
 * partage la ligne entre plusieurs sections. Un axe obligatoire sans section met la piece en
 * attente (ou refuse la saisie manuelle).</p>
 *
 * <p>A la validation, la ventilation est enregistree et, pour l'axe tenu en classe 9, reflechie en
 * partie double dans le journal {@code CAG} : charge D 93 (section) C 91, produit D 91 C 93.</p>
 */
@Component
@RequiredArgsConstructor
public class AnalyticAllocator {

    private static final Pattern PAIR = Pattern.compile("\"([A-Za-z0-9_]+)\"\\s*:\\s*\"([^\"]*)\"");

    private final AccAnalyticAxisRepository axisRepository;
    private final AccAnalyticSectionRepository sectionRepository;
    private final AccAnalyticSectionAccountRepository sectionAccountRepository;
    private final AccAnalyticRuleRepository ruleRepository;
    private final AccAllocationKeyRepository keyRepository;
    private final AccAllocationKeyLineRepository keyLineRepository;
    private final AccAnalyticAllocationRepository allocationRepository;
    private final AccAccountRepository accountRepository;
    private final AccountingSettings settings;
    @Lazy
    private final AccountingEntryService entries;

    /** Part proposee : section, pourcentage, montant, origine. */
    public record Part(String axis, String section, BigDecimal percentage, BigDecimal amount, String origin) {
    }

    // ------------------------------------------------------------------ calcul

    public boolean concerned(AccAccount account) {
        if (account == null) {
            return false;
        }
        String number = account.getNumber();
        return Boolean.TRUE.equals(account.getAnalyticRequired()) || number.startsWith("6") || number.startsWith("7");
    }

    /** Ventilation proposee d'une ligne, axe par axe (vide si aucune section trouvee pour l'axe). */
    public Map<String, List<Part>> propose(AccEntry entry, AccEntryLine line, AccAccount account, LocalDate date) {
        Map<String, List<Part>> result = new LinkedHashMap<>();
        if (!concerned(account)) {
            return result;
        }
        BigDecimal amount = line.getDebit().signum() > 0 ? line.getDebit() : line.getCredit();
        Map<String, String> requested = read(line.getAnalyticJson());
        List<AccAnalyticRule> rules = ruleRepository.findByActiveTrueOrderByPriorityAscIdAsc();
        for (AccAnalyticAxis axis : axisRepository.findByActiveTrueOrderBySortOrderAsc()) {
            String target = null;
            String origin = null;
            if (requested.containsKey(axis.getCode())) {
                target = requested.get(axis.getCode());
                origin = "INPUT";
            }
            if (target == null) {
                for (AccAnalyticRule rule : rules) {
                    if (rule.getAxisCode().equals(axis.getCode()) && matches(rule, entry, line)) {
                        target = rule.getKeyCode() != null ? "KEY:" + rule.getKeyCode() : rule.getSectionCode();
                        origin = "RULE";
                        break;
                    }
                }
            }
            if (target == null) {
                target = sectionAccountRepository.findByAxisCode(axis.getCode()).stream()
                        .filter(mapping -> line.getAccountNumber().startsWith(mapping.getAccountPrefix()))
                        .max(Comparator.comparingInt(mapping -> mapping.getAccountPrefix().length()))
                        .map(AccAnalyticSectionAccount::getSectionCode).orElse(null);
                origin = target == null ? null : "ACCOUNT";
            }
            if (target == null && StringUtils.hasText(axis.getDefaultSection())) {
                target = axis.getDefaultSection();
                origin = "DEFAULT";
            }
            if (target == null) {
                continue;
            }
            result.put(axis.getCode(), split(axis.getCode(), target, amount, origin, date));
        }
        return result;
    }

    private List<Part> split(String axis, String target, BigDecimal amount, String origin, LocalDate date) {
        if (target.startsWith("KEY:")) {
            String code = target.substring(4);
            AccAllocationKey key = keyRepository.findByCodeOrderByEffectiveFromDesc(code).stream()
                    .filter(candidate -> !candidate.getEffectiveFrom().isAfter(date)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cle de repartition " + code + " sans version en vigueur au " + date));
            List<AccAllocationKeyLine> lines = keyLineRepository.findByKeyId(key.getId());
            List<Part> parts = new ArrayList<>();
            BigDecimal left = amount;
            for (int index = 0; index < lines.size(); index++) {
                AccAllocationKeyLine keyLine = lines.get(index);
                BigDecimal part = index == lines.size() - 1 ? left
                        : amount.multiply(keyLine.getPercentage()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                left = left.subtract(part);
                parts.add(new Part(axis, resolve(axis, keyLine.getSectionCode()), keyLine.getPercentage(), part, origin + "_KEY"));
            }
            return parts;
        }
        return List.of(new Part(axis, resolve(axis, target), BigDecimal.valueOf(100), amount, origin));
    }

    /** Section fusionnee : ses imputations vont a la section qui l'a absorbee. */
    private String resolve(String axis, String code) {
        String current = code;
        for (int guard = 0; guard < 5; guard++) {
            Optional<AccAnalyticSection> section = sectionRepository.findByAxisCodeAndCode(axis, current);
            if (section.isPresent() && "MERGED".equals(section.get().getStatus()) && section.get().getMergedInto() != null) {
                current = section.get().getMergedInto();
            } else {
                break;
            }
        }
        return current;
    }

    private static boolean matches(AccAnalyticRule rule, AccEntry entry, AccEntryLine line) {
        return (rule.getAccountPrefix() == null || line.getAccountNumber().startsWith(rule.getAccountPrefix()))
                && (rule.getJournalCode() == null || rule.getJournalCode().equalsIgnoreCase(entry.getJournalCode()))
                && (rule.getSourceType() == null || rule.getSourceType().equalsIgnoreCase(entry.getSourceType()))
                && (rule.getThirdParty() == null || rule.getThirdParty().equalsIgnoreCase(line.getThirdPartyCode()));
    }

    /**
     * Problemes de ventilation d'une piece : axe obligatoire sans section, section inconnue, fermee
     * ou refusee. Vide si l'analytique est inactive.
     */
    public List<String> problems(AccEntry entry, List<AccEntryLine> lines) {
        if (!settings.analyticEnabled()) {
            return List.of();
        }
        List<String> problems = new ArrayList<>();
        List<AccAnalyticAxis> axes = axisRepository.findByActiveTrueOrderBySortOrderAsc();
        for (AccEntryLine line : lines) {
            AccAccount account = accountRepository.findByNumber(line.getAccountNumber()).orElse(null);
            if (!concerned(account)) {
                continue;
            }
            Map<String, List<Part>> parts;
            try {
                parts = propose(entry, line, account, entry.getEntryDate());
            } catch (IllegalStateException exception) {
                problems.add("Ligne " + line.getLineNo() + " : " + exception.getMessage());
                continue;
            }
            boolean charge = line.getAccountNumber().startsWith("6");
            boolean product = line.getAccountNumber().startsWith("7");
            for (AccAnalyticAxis axis : axes) {
                boolean required = "BOTH".equals(axis.getRequiredOn()) && (charge || product)
                        || "CHARGE".equals(axis.getRequiredOn()) && charge || "PRODUCT".equals(axis.getRequiredOn()) && product;
                List<Part> axisParts = parts.get(axis.getCode());
                if (axisParts == null || axisParts.isEmpty()) {
                    if (required) {
                        problems.add("Ligne " + line.getLineNo() + " (" + line.getAccountNumber() + ") : section " + axis.getLabel() + " obligatoire");
                    }
                    continue;
                }
                for (Part part : axisParts) {
                    Optional<AccAnalyticSection> section = sectionRepository.findByAxisCodeAndCode(axis.getCode(), part.section());
                    if (section.isEmpty()) {
                        problems.add("Ligne " + line.getLineNo() + " : section " + part.section() + " inconnue sur l'axe " + axis.getLabel());
                    } else if (!Set.of("ACTIVE", "PENDING_APPROVAL").contains(section.get().getStatus())) {
                        problems.add("Ligne " + line.getLineNo() + " : section " + part.section() + " fermee (" + section.get().getStatus() + ")");
                    } else if (section.get().getValidTo() != null && section.get().getValidTo().isBefore(entry.getEntryDate())) {
                        problems.add("Ligne " + line.getLineNo() + " : section " + part.section() + " plus valable au " + entry.getEntryDate());
                    }
                }
            }
        }
        return problems;
    }

    // ------------------------------------------------------------------ enregistrement

    /** Enregistre la ventilation d'une piece validee et passe son reflet en classe 9. */
    public void persist(AccEntry entry, List<AccEntryLine> lines) {
        if (!settings.analyticEnabled() || allocationRepository.existsByEntryId(entry.getId())) {
            return;
        }
        String reflectedAxis = settings.text("accounting.analytic.class9-axis", "ACTIVITE");
        Map<String, BigDecimal> reflect = new TreeMap<>();
        for (AccEntryLine line : lines) {
            AccAccount account = accountRepository.findByNumber(line.getAccountNumber()).orElse(null);
            if (!concerned(account)) {
                continue;
            }
            Map<String, List<Part>> parts = propose(entry, line, account, entry.getEntryDate());
            BigDecimal lineAmount = line.getDebit().signum() > 0 ? line.getDebit() : line.getCredit();
            for (List<Part> axisParts : parts.values()) {
                for (Part part : axisParts) {
                    allocationRepository.save(AccAnalyticAllocation.builder().entryLineId(line.getId()).entryId(entry.getId())
                            .axisCode(part.axis()).sectionCode(part.section()).percentage(part.percentage()).amount(part.amount())
                            .origin(part.origin()).build());
                    if (part.axis().equals(reflectedAxis)) {
                        // Debit d'une charge ou d'un produit : le centre (93) est debite ; credit : credite.
                        BigDecimal signed = line.getDebit().signum() > 0 ? part.amount() : part.amount().negate();
                        reflect.merge(part.section(), signed, BigDecimal::add);
                    }
                }
            }
            if (lineAmount.signum() == 0) {
                continue;
            }
        }
        if (reflect.isEmpty() || !StringUtils.hasText(reflectedAxis)) {
            return;
        }
        String centres = settings.text("accounting.analytic.centre-account", "93");
        String reflected = settings.text("accounting.analytic.reflected-account", "91");
        List<AccountingRequests.EntryLine> cagLines = new ArrayList<>();
        BigDecimal net = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> section : reflect.entrySet()) {
            BigDecimal value = section.getValue();
            if (value.signum() == 0) {
                continue;
            }
            cagLines.add(new AccountingRequests.EntryLine(centres, null, "Section " + section.getKey(), value.signum() > 0 ? value : null,
                    value.signum() < 0 ? value.negate() : null, null, null, null, null, null, null, section.getKey()));
            net = net.add(value);
        }
        if (net.signum() != 0) {
            cagLines.add(new AccountingRequests.EntryLine(reflected, null, "Reflet de " + entry.getEntryNumber(), net.signum() < 0 ? net.negate() : null,
                    net.signum() > 0 ? net : null, null, null, null, null, null, null, entry.getEntryNumber()));
        }
        if (cagLines.size() < 2) {
            return;
        }
        LocalDate date = entry.getEntryDate();
        Optional<LocalDate> last = entries.lastValidatedDate("CAG", entry.getFiscalYearCode());
        if (last.isPresent() && last.get().isAfter(date)) {
            date = last.get();
        }
        entries.post(new AccountingEntryService.AutoPost("CAG", date, date.equals(entry.getEntryDate()) ? null : entry.getEntryDate(),
                "Reflet analytique de " + entry.getEntryNumber(), EntryOrigin.PLATFORM, "ANALYTIC_REFLECTION", entry.getEntryNumber(), cagLines,
                "SYSTEM", true, null));
    }

    // ------------------------------------------------------------------ format

    public static String write(Map<String, String> analytic) {
        if (analytic == null || analytic.isEmpty()) {
            return null;
        }
        StringBuilder out = new StringBuilder("{");
        analytic.forEach((axis, section) -> {
            if (StringUtils.hasText(axis) && StringUtils.hasText(section)) {
                if (out.length() > 1) {
                    out.append(',');
                }
                out.append('"').append(axis.trim().toUpperCase(Locale.ROOT).replace("\"", "")).append("\":\"")
                        .append(section.trim().replace("\"", "")).append('"');
            }
        });
        return out.length() == 1 ? null : out.append('}').toString();
    }

    public static Map<String, String> read(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (!StringUtils.hasText(json)) {
            return result;
        }
        Matcher matcher = PAIR.matcher(json);
        while (matcher.find()) {
            result.put(matcher.group(1).toUpperCase(Locale.ROOT), matcher.group(2));
        }
        return result;
    }
}
