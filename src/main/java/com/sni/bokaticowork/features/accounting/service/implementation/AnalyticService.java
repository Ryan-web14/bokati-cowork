package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parametrage analytique (lot F9) : axes, sections et postes de depense definis par l'utilisateur
 * (creation immediate ou soumise a validation, fermeture, fusion, deplacement), correspondance
 * avec les comptes, regles, cles de repartition ; reclassement trace ; repartition secondaire.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AnalyticService {

    static final String MANAGE = "ANALYTIC_SETTINGS:MANAGE";

    private final AccAnalyticAxisRepository axisRepository;
    private final AccAnalyticSectionRepository sectionRepository;
    private final AccAnalyticSectionAccountRepository sectionAccountRepository;
    private final AccAnalyticRuleRepository ruleRepository;
    private final AccAllocationKeyRepository keyRepository;
    private final AccAllocationKeyLineRepository keyLineRepository;
    private final AccAnalyticAllocationRepository allocationRepository;
    private final AccAnalyticReclassRepository reclassRepository;
    private final AccAnalyticDistributionRepository distributionRepository;
    private final AccEntryLineRepository lineRepository;
    private final AccEntryRepository entryRepository;
    private final AccountingPeriodService periods;
    private final AccountingEntryService entries;
    private final AccountingSettings settings;
    private final CashActorResolver actors;

    // ------------------------------------------------------------------ requetes

    public record AxisInput(String code, String label, String requiredOn, String defaultSection, Integer sortOrder, Boolean active) {
    }

    public record SectionInput(String code, String label, String parentCode, String family, String costBehavior, Boolean auxiliary,
                               String responsible, LocalDate validFrom, LocalDate validTo, String color, Integer sortOrder, List<String> accounts) {
    }

    public record RuleInput(Integer priority, String axisCode, String accountPrefix, String journalCode, String sourceType, String thirdParty,
                            String sectionCode, String keyCode, String label, Boolean active) {
    }

    public record KeyLine(String sectionCode, BigDecimal percentage) {
    }

    public record KeyInput(String code, String label, String axisCode, LocalDate effectiveFrom, List<KeyLine> lines) {
    }

    public record Reclass(Long entryLineId, String axisCode, String fromSection, String toSection, BigDecimal amount, String reason) {
    }

    // ------------------------------------------------------------------ reponses

    public record Axis(String code, String label, String requiredOn, String defaultSection, Integer sortOrder, Boolean active, long sections) {
    }

    public record Section(String axisCode, String code, String label, String parentCode, String family, String costBehavior, Boolean auxiliary,
                          String responsible, LocalDate validFrom, LocalDate validTo, String status, String mergedInto, String color,
                          Integer sortOrder, String createdBy, String approvedBy, List<String> accounts, List<Section> children) {
    }

    public record Key(String code, String label, String axisCode, LocalDate effectiveFrom, List<KeyLine> lines) {
    }

    public record Allocation(Long entryLineId, String axisCode, String sectionCode, BigDecimal percentage, BigDecimal amount, String origin) {
    }

    // ------------------------------------------------------------------ axes

    @Transactional(readOnly = true)
    public List<Axis> axes() {
        return axisRepository.findAllByOrderBySortOrderAsc().stream()
                .map(axis -> new Axis(axis.getCode(), axis.getLabel(), axis.getRequiredOn(), axis.getDefaultSection(), axis.getSortOrder(),
                        axis.getActive(), sectionRepository.findByAxisCodeOrderBySortOrderAscCodeAsc(axis.getCode()).size()))
                .toList();
    }

    public Axis saveAxis(AxisInput input) {
        manage();
        String code = code(input.code());
        AccAnalyticAxis axis = axisRepository.findByCode(code).orElseGet(() -> AccAnalyticAxis.builder().code(code).build());
        axis.setLabel(StringUtils.hasText(input.label()) ? input.label().trim() : code);
        String required = input.requiredOn() == null ? "NONE" : input.requiredOn().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("NONE", "CHARGE", "PRODUCT", "BOTH").contains(required)) {
            throw new BadRequestException("Obligation : NONE, CHARGE, PRODUCT ou BOTH");
        }
        axis.setRequiredOn(required);
        axis.setDefaultSection(StringUtils.hasText(input.defaultSection()) ? code(input.defaultSection()) : null);
        axis.setSortOrder(input.sortOrder() == null ? 0 : input.sortOrder());
        axis.setActive(input.active() == null || input.active());
        axisRepository.save(axis);
        return axes().stream().filter(view -> view.code().equals(code)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ sections

    /** Arbre des sections d'un axe. */
    @Transactional(readOnly = true)
    public List<Section> tree(String axisCode, boolean includeClosed) {
        List<AccAnalyticSection> all = sectionRepository.findByAxisCodeOrderBySortOrderAscCodeAsc(code(axisCode)).stream()
                .filter(section -> includeClosed || !Set.of("CLOSED", "MERGED", "REJECTED").contains(section.getStatus())).toList();
        Map<String, List<String>> accounts = sectionAccountRepository.findByAxisCode(code(axisCode)).stream()
                .collect(Collectors.groupingBy(AccAnalyticSectionAccount::getSectionCode, Collectors.mapping(AccAnalyticSectionAccount::getAccountPrefix, Collectors.toList())));
        Set<String> codes = all.stream().map(AccAnalyticSection::getCode).collect(Collectors.toSet());
        return all.stream().filter(section -> section.getParentCode() == null || !codes.contains(section.getParentCode()))
                .map(section -> node(section, all, accounts)).toList();
    }

    private Section node(AccAnalyticSection section, List<AccAnalyticSection> all, Map<String, List<String>> accounts) {
        List<Section> children = all.stream().filter(child -> section.getCode().equals(child.getParentCode())).map(child -> node(child, all, accounts)).toList();
        return view(section, accounts.getOrDefault(section.getCode(), List.of()), children);
    }

    private static Section view(AccAnalyticSection section, List<String> accounts, List<Section> children) {
        return new Section(section.getAxisCode(), section.getCode(), section.getLabel(), section.getParentCode(), section.getFamily(),
                section.getCostBehavior(), section.getAuxiliary(), section.getResponsible(), section.getValidFrom(), section.getValidTo(),
                section.getStatus(), section.getMergedInto(), section.getColor(), section.getSortOrder(), section.getCreatedBy(),
                section.getApprovedBy(), accounts, children);
    }

    /**
     * Cree une section ou un poste. Administrateur : immediat. Responsable de centre, dans sa branche,
     * ou proposition a la volee : en attente de validation si {@code accounting.analytic.creation-mode}
     * vaut {@code AFTER_APPROVAL} ou si l'auteur n'a pas le droit de parametrage.
     */
    public Section createSection(String axisCode, SectionInput input) {
        String axis = axis(axisCode).getCode();
        String actor = actors.resolve("SYSTEM");
        boolean admin = actors.hasAuthority(MANAGE);
        if (!admin && !actors.hasAuthority("ANALYTIC_ITEM:CREATE")) {
            throw new ForbiddenException("Creer un poste analytique demande le droit ANALYTIC_ITEM:CREATE");
        }
        String code = StringUtils.hasText(input.code()) ? code(input.code()) : proposeCode(axis, input.label());
        if (sectionRepository.findByAxisCodeAndCode(axis, code).isPresent()) {
            throw new BadRequestException("La section " + code + " existe deja sur l'axe " + axis);
        }
        if (!StringUtils.hasText(input.label())) {
            throw new BadRequestException("Donnez un libelle a la section");
        }
        List<String> duplicates = sectionRepository.findByAxisCodeOrderBySortOrderAscCodeAsc(axis).stream()
                .filter(section -> section.getLabel().equalsIgnoreCase(input.label().trim())).map(AccAnalyticSection::getCode).toList();
        if (!duplicates.isEmpty()) {
            throw new BadRequestException("Une section porte deja ce libelle : " + String.join(", ", duplicates));
        }
        if (StringUtils.hasText(input.parentCode())) {
            AccAnalyticSection parent = section(axis, input.parentCode());
            if (!admin && !CashActorResolver.samePerson(actor, parent.getResponsible())) {
                throw new ForbiddenException("Un responsable de centre cree des postes dans sa propre branche");
            }
        }
        boolean approval = !admin || "AFTER_APPROVAL".equalsIgnoreCase(settings.text("accounting.analytic.creation-mode", "IMMEDIATE"));
        AccAnalyticSection section = AccAnalyticSection.builder().axisCode(axis).code(code).label(input.label().trim())
                .status(approval && !admin ? "PENDING_APPROVAL" : "ACTIVE").createdBy(actor).approvedBy(approval && !admin ? null : actor).build();
        apply(section, input);
        sectionRepository.save(section);
        mapAccounts(axis, code, input.accounts());
        return view(section, accounts(axis, code), List.of());
    }

    public Section updateSection(String axisCode, String code, SectionInput input) {
        manage();
        AccAnalyticSection section = section(axisCode, code);
        if (StringUtils.hasText(input.label())) {
            section.setLabel(input.label().trim());
        }
        apply(section, input);
        sectionRepository.save(section);
        if (input.accounts() != null) {
            sectionAccountRepository.findByAxisCodeAndSectionCode(section.getAxisCode(), section.getCode()).forEach(sectionAccountRepository::delete);
            sectionAccountRepository.flush();
            mapAccounts(section.getAxisCode(), section.getCode(), input.accounts());
        }
        return view(section, accounts(section.getAxisCode(), section.getCode()), List.of());
    }

    private void apply(AccAnalyticSection section, SectionInput input) {
        if (input.parentCode() != null) {
            String parent = StringUtils.hasText(input.parentCode()) ? code(input.parentCode()) : null;
            if (parent != null) {
                section(section.getAxisCode(), parent);
                if (parent.equals(section.getCode()) || descendants(section.getAxisCode(), section.getCode()).contains(parent)) {
                    throw new BadRequestException("Une section ne se place pas sous elle-meme ni sous l'une de ses sous-sections");
                }
            }
            section.setParentCode(parent);
        }
        if (input.family() != null) section.setFamily(blank(input.family()));
        if (input.costBehavior() != null) {
            String behavior = blank(input.costBehavior());
            if (behavior != null && !Set.of("FIXED", "VARIABLE").contains(behavior.toUpperCase(Locale.ROOT))) {
                throw new BadRequestException("Nature : FIXED ou VARIABLE");
            }
            section.setCostBehavior(behavior == null ? null : behavior.toUpperCase(Locale.ROOT));
        }
        if (input.auxiliary() != null) section.setAuxiliary(input.auxiliary());
        if (input.responsible() != null) section.setResponsible(blank(input.responsible()));
        if (input.validFrom() != null) section.setValidFrom(input.validFrom());
        if (input.validTo() != null) section.setValidTo(input.validTo());
        if (input.color() != null) section.setColor(blank(input.color()));
        if (input.sortOrder() != null) section.setSortOrder(input.sortOrder());
    }

    private void mapAccounts(String axis, String code, List<String> accounts) {
        if (accounts == null) {
            return;
        }
        for (String prefix : accounts) {
            if (!StringUtils.hasText(prefix) || !prefix.trim().matches("[1-9][0-9]*")) {
                throw new BadRequestException("Prefixe de compte invalide : " + prefix);
            }
            sectionAccountRepository.findByAxisCode(axis).stream().filter(mapping -> mapping.getAccountPrefix().equals(prefix.trim()))
                    .forEach(sectionAccountRepository::delete);
            sectionAccountRepository.flush();
            sectionAccountRepository.save(AccAnalyticSectionAccount.builder().axisCode(axis).sectionCode(code).accountPrefix(prefix.trim()).build());
        }
    }

    public Section approve(String axisCode, String code, boolean approve) {
        manage();
        AccAnalyticSection section = section(axisCode, code);
        if (!"PENDING_APPROVAL".equals(section.getStatus())) {
            throw new BadRequestException("La section " + code + " n'attend pas de validation");
        }
        section.setStatus(approve ? "ACTIVE" : "REJECTED");
        section.setApprovedBy(actors.resolve("SYSTEM"));
        sectionRepository.save(section);
        return view(section, accounts(section.getAxisCode(), code), List.of());
    }

    /** Fermer : plus d'imputation, historique conserve. Rouvrir l'inverse. */
    public Section close(String axisCode, String code, boolean close) {
        manage();
        AccAnalyticSection section = section(axisCode, code);
        section.setStatus(close ? "CLOSED" : "ACTIVE");
        sectionRepository.save(section);
        return view(section, accounts(section.getAxisCode(), code), List.of());
    }

    /**
     * Fusion : la section absorbee est fermee, ses futures imputations vont a la cible ; avec
     * {@code restate}, ses ventilations passees sont aussi deplacees (sinon les etats passes restent).
     */
    public Section merge(String axisCode, String code, String into, boolean restate) {
        manage();
        AccAnalyticSection source = section(axisCode, code);
        AccAnalyticSection target = section(axisCode, into);
        if (source.getCode().equals(target.getCode())) {
            throw new BadRequestException("Une section ne se fusionne pas avec elle-meme");
        }
        source.setStatus("MERGED");
        source.setMergedInto(target.getCode());
        sectionRepository.save(source);
        if (restate) {
            allocationRepository.findByAxisCodeAndSectionCode(source.getAxisCode(), source.getCode()).forEach(allocation -> {
                allocation.setSectionCode(target.getCode());
                allocationRepository.save(allocation);
            });
        }
        sectionRepository.findByAxisCodeOrderBySortOrderAscCodeAsc(source.getAxisCode()).stream()
                .filter(child -> source.getCode().equals(child.getParentCode())).forEach(child -> {
                    child.setParentCode(target.getCode());
                    sectionRepository.save(child);
                });
        return view(target, accounts(target.getAxisCode(), target.getCode()), List.of());
    }

    @Transactional(readOnly = true)
    public List<Section> pending() {
        return sectionRepository.findByStatusOrderByCreatedAtAsc("PENDING_APPROVAL").stream()
                .map(section -> view(section, accounts(section.getAxisCode(), section.getCode()), List.of())).toList();
    }

    // ------------------------------------------------------------------ regles et cles

    @Transactional(readOnly = true)
    public List<AccAnalyticRule> rules() {
        return ruleRepository.findAllByOrderByPriorityAscIdAsc();
    }

    public AccAnalyticRule saveRule(Long id, RuleInput input) {
        manage();
        AccAnalyticRule rule = id == null ? AccAnalyticRule.builder().createdBy(actors.resolve("SYSTEM")).build()
                : ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Regle analytique introuvable : " + id));
        String axis = axis(input.axisCode()).getCode();
        if (!StringUtils.hasText(input.sectionCode()) && !StringUtils.hasText(input.keyCode())) {
            throw new BadRequestException("Une regle cible une section ou une cle de repartition");
        }
        if (StringUtils.hasText(input.sectionCode())) {
            section(axis, input.sectionCode());
        }
        if (StringUtils.hasText(input.keyCode()) && keyRepository.findByCodeOrderByEffectiveFromDesc(code(input.keyCode())).isEmpty()) {
            throw new BadRequestException("Cle de repartition inconnue : " + input.keyCode());
        }
        rule.setPriority(input.priority() == null ? 100 : input.priority());
        rule.setAxisCode(axis);
        rule.setAccountPrefix(blank(input.accountPrefix()));
        rule.setJournalCode(blank(input.journalCode()) == null ? null : input.journalCode().trim().toUpperCase(Locale.ROOT));
        rule.setSourceType(blank(input.sourceType()) == null ? null : input.sourceType().trim().toUpperCase(Locale.ROOT));
        rule.setThirdParty(blank(input.thirdParty()));
        rule.setSectionCode(StringUtils.hasText(input.sectionCode()) ? code(input.sectionCode()) : null);
        rule.setKeyCode(StringUtils.hasText(input.keyCode()) ? code(input.keyCode()) : null);
        rule.setLabel(blank(input.label()));
        rule.setActive(input.active() == null || input.active());
        return ruleRepository.save(rule);
    }

    public void deleteRule(Long id) {
        manage();
        ruleRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Key> keys() {
        return keyRepository.findAllByOrderByCodeAscEffectiveFromDesc().stream()
                .map(key -> new Key(key.getCode(), key.getLabel(), key.getAxisCode(), key.getEffectiveFrom(),
                        keyLineRepository.findByKeyId(key.getId()).stream().map(line -> new KeyLine(line.getSectionCode(), line.getPercentage())).toList()))
                .toList();
    }

    /** Nouvelle version d'une cle (date d'effet) : les parts totalisent 100 %. */
    public Key saveKey(KeyInput input) {
        manage();
        String axis = axis(input.axisCode()).getCode();
        if (input.lines() == null || input.lines().isEmpty()) {
            throw new BadRequestException("Une cle a au moins une section");
        }
        BigDecimal total = input.lines().stream().map(KeyLine::percentage).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new BadRequestException("Les parts d'une cle totalisent 100 % (ici " + total.stripTrailingZeros().toPlainString() + " %)");
        }
        input.lines().forEach(line -> section(axis, line.sectionCode()));
        String code = code(input.code());
        LocalDate from = input.effectiveFrom() == null ? LocalDate.now() : input.effectiveFrom();
        keyRepository.findByCodeOrderByEffectiveFromDesc(code).stream().filter(key -> key.getEffectiveFrom().equals(from)).findFirst()
                .ifPresent(existing -> {
                    keyLineRepository.findByKeyId(existing.getId()).forEach(keyLineRepository::delete);
                    keyRepository.delete(existing);
                    keyRepository.flush();
                });
        AccAllocationKey key = keyRepository.save(AccAllocationKey.builder().code(code).label(StringUtils.hasText(input.label()) ? input.label().trim() : code)
                .axisCode(axis).effectiveFrom(from).createdBy(actors.resolve("SYSTEM")).build());
        input.lines().forEach(line -> keyLineRepository.save(AccAllocationKeyLine.builder().keyId(key.getId()).sectionCode(code(line.sectionCode()))
                .percentage(line.percentage()).build()));
        return keys().stream().filter(view -> view.code().equals(code) && view.effectiveFrom().equals(from)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ reclassement

    @Transactional(readOnly = true)
    public List<Allocation> allocations(Long entryId) {
        return allocationRepository.findByEntryId(entryId).stream()
                .map(allocation -> new Allocation(allocation.getEntryLineId(), allocation.getAxisCode(), allocation.getSectionCode(),
                        allocation.getPercentage(), allocation.getAmount(), allocation.getOrigin())).toList();
    }

    /**
     * Reclassement : une erreur de section se corrige sans toucher a l'ecriture generale. La
     * ventilation est deplacee, le reclassement trace, et, sur l'axe tenu en classe 9, une ecriture
     * D 93 nouvelle section / C 93 ancienne section est passee au journal {@code CAG}.
     */
    public Allocation reclass(Reclass input) {
        if (!actors.hasAuthority("ANALYTIC:RECLASS") && !actors.hasAuthority(MANAGE)) {
            throw new ForbiddenException("Reclasser demande le droit ANALYTIC:RECLASS");
        }
        if (!StringUtils.hasText(input.reason())) {
            throw new BadRequestException("Indiquez le motif du reclassement");
        }
        String axis = axis(input.axisCode()).getCode();
        AccAnalyticSection target = section(axis, input.toSection());
        if (!"ACTIVE".equals(target.getStatus())) {
            throw new BadRequestException("La section " + target.getCode() + " n'accepte pas d'imputation (" + target.getStatus() + ")");
        }
        AccAnalyticAllocation source = allocationRepository.findByEntryLineId(input.entryLineId()).stream()
                .filter(allocation -> allocation.getAxisCode().equals(axis) && allocation.getSectionCode().equals(code(input.fromSection())))
                .findFirst().orElseThrow(() -> new BadRequestException("La ligne n'est pas ventilee sur " + input.fromSection()));
        BigDecimal amount = input.amount() == null ? source.getAmount() : input.amount().setScale(4, RoundingMode.HALF_UP);
        if (amount.signum() <= 0 || amount.compareTo(source.getAmount()) > 0) {
            throw new BadRequestException("Montant reclasse entre 0 et " + source.getAmount().stripTrailingZeros().toPlainString());
        }
        AccEntryLine line = lineRepository.findById(input.entryLineId()).orElseThrow();
        AccEntry entry = entryRepository.findById(line.getEntryId()).orElseThrow();
        BigDecimal lineAmount = line.getDebit().signum() > 0 ? line.getDebit() : line.getCredit();
        BigDecimal pct = amount.multiply(BigDecimal.valueOf(100)).divide(lineAmount, 4, RoundingMode.HALF_UP);
        source.setAmount(source.getAmount().subtract(amount));
        source.setPercentage(source.getPercentage().subtract(pct).max(BigDecimal.ZERO));
        if (source.getAmount().signum() == 0) {
            allocationRepository.delete(source);
        } else {
            allocationRepository.save(source);
        }
        AccAnalyticAllocation moved = allocationRepository.save(AccAnalyticAllocation.builder().entryLineId(line.getId()).entryId(entry.getId())
                .axisCode(axis).sectionCode(target.getCode()).percentage(pct).amount(amount).origin("RECLASS").build());
        String number = null;
        if (axis.equals(settings.text("accounting.analytic.class9-axis", "ACTIVITE"))) {
            String centres = settings.text("accounting.analytic.centre-account", "93");
            boolean debit = line.getDebit().signum() > 0;
            List<AccountingRequests.EntryLine> lines = List.of(
                    new AccountingRequests.EntryLine(centres, null, "Reclassement vers " + target.getCode(), debit ? amount : null, debit ? null : amount,
                            null, null, null, null, null, null, target.getCode()),
                    new AccountingRequests.EntryLine(centres, null, "Reclassement depuis " + input.fromSection(), debit ? null : amount, debit ? amount : null,
                            null, null, null, null, null, null, code(input.fromSection())));
            LocalDate date = LocalDate.now(PostingEngine.AccountingClock.ZONE);
            AccPeriod period = periods.firstOpenOnOrAfter(date);
            if (period.getStartDate().isAfter(date)) {
                date = period.getStartDate();
            }
            AccEntry posted = entries.post(new AccountingEntryService.AutoPost("CAG", date, null, "Reclassement analytique : " + input.reason().trim(),
                    EntryOrigin.MANUAL, "ANALYTIC_RECLASS", entry.getEntryNumber(), lines, actors.resolve("SYSTEM"), true, null));
            number = posted.getStatus() == EntryStatus.VALIDATED ? posted.getEntryNumber() : posted.getProvisionalNumber();
        }
        reclassRepository.save(AccAnalyticReclass.builder().entryLineId(line.getId()).axisCode(axis).fromSection(code(input.fromSection()))
                .toSection(target.getCode()).amount(amount).reason(input.reason().trim()).entryNumber(number).createdBy(actors.resolve("SYSTEM")).build());
        return new Allocation(moved.getEntryLineId(), axis, moved.getSectionCode(), moved.getPercentage(), moved.getAmount(), moved.getOrigin());
    }

    // ------------------------------------------------------------------ repartition secondaire

    /**
     * Repartition secondaire d'une periode : les charges des sections auxiliaires d'un axe sont
     * deversees sur les sections principales selon la cle donnee. Rejouable : la repartition
     * precedente de la periode est remplacee.
     */
    public List<AccAnalyticDistribution> distribute(String periodCode, String axisCode, String keyCode) {
        manage();
        String axis = axis(axisCode).getCode();
        AccPeriod period = periods.period(periodCode);
        List<AccAllocationKeyLine> keyLines = keyRepository.findByCodeOrderByEffectiveFromDesc(code(keyCode)).stream()
                .filter(key -> !key.getEffectiveFrom().isAfter(period.getEndDate())).findFirst()
                .map(key -> keyLineRepository.findByKeyId(key.getId()))
                .orElseThrow(() -> new BadRequestException("Cle de repartition " + keyCode + " sans version en vigueur"));
        distributionRepository.deleteByPeriodCodeAndAxisCode(period.getCode(), axis);
        distributionRepository.flush();
        Set<String> auxiliary = sectionRepository.findByAxisCodeOrderBySortOrderAscCodeAsc(axis).stream()
                .filter(section -> Boolean.TRUE.equals(section.getAuxiliary())).map(AccAnalyticSection::getCode).collect(Collectors.toSet());
        Map<String, BigDecimal> charges = new TreeMap<>();
        for (AccAnalyticAllocation allocation : allocationRepository.findAll()) {
            if (!allocation.getAxisCode().equals(axis) || !auxiliary.contains(allocation.getSectionCode())) {
                continue;
            }
            AccEntryLine line = lineRepository.findById(allocation.getEntryLineId()).orElse(null);
            AccEntry entry = line == null ? null : entryRepository.findById(line.getEntryId()).orElse(null);
            if (entry == null || !period.getCode().equals(entry.getPeriodCode()) || entry.getStatus() != EntryStatus.VALIDATED
                    || !line.getAccountNumber().startsWith("6")) {
                continue;
            }
            BigDecimal signed = line.getDebit().signum() > 0 ? allocation.getAmount() : allocation.getAmount().negate();
            charges.merge(allocation.getSectionCode(), signed, BigDecimal::add);
        }
        String actor = actors.resolve("SYSTEM");
        List<AccAnalyticDistribution> result = new ArrayList<>();
        charges.forEach((from, total) -> {
            BigDecimal left = total;
            List<AccAllocationKeyLine> targets = keyLines.stream().filter(line -> !line.getSectionCode().equals(from)).toList();
            BigDecimal base = targets.stream().map(AccAllocationKeyLine::getPercentage).reduce(BigDecimal.ZERO, BigDecimal::add);
            for (int index = 0; index < targets.size(); index++) {
                AccAllocationKeyLine target = targets.get(index);
                BigDecimal part = index == targets.size() - 1 ? left
                        : total.multiply(target.getPercentage()).divide(base, 4, RoundingMode.HALF_UP);
                left = left.subtract(part);
                result.add(distributionRepository.save(AccAnalyticDistribution.builder().periodCode(period.getCode()).axisCode(axis)
                        .fromSection(from).toSection(target.getSectionCode()).keyCode(code(keyCode)).amount(part).createdBy(actor).build()));
            }
        });
        return result;
    }

    // ------------------------------------------------------------------ outils

    private AccAnalyticAxis axis(String code) {
        return axisRepository.findByCode(code(code)).orElseThrow(() -> new ResourceNotFoundException("Axe analytique introuvable : " + code));
    }

    AccAnalyticSection section(String axis, String code) {
        return sectionRepository.findByAxisCodeAndCode(code(axis), code(code))
                .orElseThrow(() -> new ResourceNotFoundException("Section " + code + " introuvable sur l'axe " + axis));
    }

    private List<String> accounts(String axis, String code) {
        return sectionAccountRepository.findByAxisCodeAndSectionCode(axis, code).stream().map(AccAnalyticSectionAccount::getAccountPrefix).toList();
    }

    private Set<String> descendants(String axis, String code) {
        Set<String> result = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(List.of(code));
        List<AccAnalyticSection> all = sectionRepository.findByAxisCodeOrderBySortOrderAscCodeAsc(axis);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            all.stream().filter(section -> current.equals(section.getParentCode())).forEach(child -> {
                if (result.add(child.getCode())) {
                    queue.add(child.getCode());
                }
            });
        }
        return result;
    }

    private String proposeCode(String axis, String label) {
        String base = java.text.Normalizer.normalize(label == null ? "SECTION" : label, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_|_$", "");
        base = base.isEmpty() ? "SECTION" : base.length() > 40 ? base.substring(0, 40) : base;
        String code = base;
        for (int index = 2; sectionRepository.findByAxisCodeAndCode(axis, code).isPresent(); index++) {
            code = base + "_" + index;
        }
        return code;
    }

    private void manage() {
        if (!actors.hasAuthority(MANAGE)) {
            throw new ForbiddenException("Ce parametrage demande le droit " + MANAGE);
        }
    }

    static String code(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException("Code manquant");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
