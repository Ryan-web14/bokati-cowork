package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.repository.*;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;

/**
 * Gestion budgetaire (lot F16) : nomenclature des postes, versions et lignes de budget,
 * mensualisation, circuit (brouillon, soumis, consolide, actif), revisions, gel, virements de
 * credits, previsions de depenses.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class BudgetService {

    private final AccBudgetItemRepository itemRepository;
    private final AccBudgetItemMappingRepository mappingRepository;
    private final AccBudgetRepository budgetRepository;
    private final AccBudgetLineRepository lineRepository;
    private final AccBudgetLineMonthRepository monthRepository;
    private final AccBudgetForecastRepository forecastRepository;
    private final AccBudgetTransferRepository transferRepository;
    private final AccFiscalYearRepository fiscalYearRepository;
    private final BudgetEngine engine;
    private final SequenceGeneratorFacade sequences;
    private final CashActorResolver actors;
    private final DataSource dataSource;

    // ------------------------------------------------------------------ requetes et vues

    public record ItemInput(String code, String label, String parentCode, String itemType, String responsible, String controlLevel,
                            BigDecimal tolerancePct, Boolean controlAtParent, List<String> accounts, List<String> expenseItems, List<String> natures) {
    }

    public record Item(String code, String label, String parentCode, String itemType, String responsible, String controlLevel,
                       BigDecimal tolerancePct, Boolean controlAtParent, String status, String mergedInto, List<String> accounts,
                       List<String> expenseItems, List<String> natures) {
    }

    public record CreateBudget(String fiscalYearCode, String label, String reason, String basis, BigDecimal evolutionPct) {
    }

    public record LineInput(String itemCode, String sectionCode, BigDecimal annualAmount, String profile, List<BigDecimal> months,
                            String controlLevel, BigDecimal tolerancePct, String responsible) {
    }

    public record Line(Long id, String itemCode, String itemLabel, String sectionCode, BigDecimal annualAmount, List<BigDecimal> months,
                       String controlLevel, BigDecimal tolerancePct, String responsible, Boolean frozen, BigDecimal committed, BigDecimal realized,
                       BigDecimal available, BigDecimal consumptionPct) {
    }

    public record Budget(Long id, String fiscalYearCode, Integer version, String label, String status, String reason, String submittedBy,
                         String consolidatedBy, String approvedBy, Instant approvedAt, String createdBy, BigDecimal totalCharges,
                         BigDecimal totalProducts, List<Line> lines) {
    }

    public record TransferInput(Long fromLineId, Long toLineId, BigDecimal amount, String reason) {
    }

    public record ForecastInput(String itemCode, String sectionCode, String subject, BigDecimal amount, String plannedMonth, String recurrence,
                                Integer occurrences, String supplier, String responsible, String priority) {
    }

    // ------------------------------------------------------------------ postes budgetaires

    @Transactional(readOnly = true)
    public List<Item> items() {
        return itemRepository.findAllByOrderByCodeAsc().stream().map(this::view).toList();
    }

    public Item saveItem(ItemInput input) {
        String code = AnalyticService.code(input.code());
        boolean manager = actors.hasAuthority("BUDGET_ITEM:MANAGE");
        AccBudgetItem item = itemRepository.findByCode(code).orElse(null);
        if (item == null) {
            if (!manager && !actors.hasAuthority("BUDGET:PREPARE")) {
                throw new ForbiddenException("Creer un poste budgetaire demande BUDGET_ITEM:MANAGE (ou BUDGET:PREPARE, avec validation)");
            }
            item = AccBudgetItem.builder().code(code).createdBy(actors.resolve("SYSTEM")).status(manager ? "ACTIVE" : "PENDING_APPROVAL").build();
        } else if (!manager) {
            throw new ForbiddenException("Modifier un poste budgetaire demande BUDGET_ITEM:MANAGE");
        }
        if (!StringUtils.hasText(input.label())) {
            throw new BadRequestException("Donnez un libelle au poste budgetaire");
        }
        item.setLabel(input.label().trim());
        if (StringUtils.hasText(input.parentCode())) {
            String parent = AnalyticService.code(input.parentCode());
            if (parent.equals(code) || engine.descendants(code).contains(parent) || itemRepository.findByCode(parent).isEmpty()) {
                throw new BadRequestException("Parent invalide : " + parent);
            }
            item.setParentCode(parent);
        } else if (input.parentCode() != null) {
            item.setParentCode(null);
        }
        if (input.itemType() != null) item.setItemType(oneOf(input.itemType(), Set.of("CHARGE", "PRODUCT", "INVESTMENT", "TREASURY"), "Type"));
        if (input.controlLevel() != null) item.setControlLevel(oneOf(input.controlLevel(), Set.of("BLOCKING", "ALERT", "INFO"), "Niveau de controle"));
        if (input.tolerancePct() != null) item.setTolerancePct(input.tolerancePct());
        if (input.controlAtParent() != null) item.setControlAtParent(input.controlAtParent());
        if (input.responsible() != null) item.setResponsible(StringUtils.hasText(input.responsible()) ? input.responsible().trim() : null);
        itemRepository.save(item);
        map(code, "ACCOUNT", input.accounts());
        map(code, "EXPENSE_ITEM", input.expenseItems());
        map(code, "NATURE", input.natures());
        return view(item);
    }

    private void map(String code, String kind, List<String> values) {
        if (values == null) {
            return;
        }
        mappingRepository.findByItemCode(code).stream().filter(m -> m.getKind().equals(kind)).forEach(mappingRepository::delete);
        mappingRepository.flush();
        for (String raw : values) {
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            String value = "ACCOUNT".equals(kind) ? raw.trim() : raw.trim().toUpperCase(Locale.ROOT);
            mappingRepository.findByKindAndValue(kind, value).ifPresent(existing -> {
                throw new BadRequestException(value + " alimente deja le poste " + existing.getItemCode());
            });
            mappingRepository.save(AccBudgetItemMapping.builder().itemCode(code).kind(kind).value(value).build());
        }
    }

    public Item decideItem(String code, String decision, String into) {
        if (!actors.hasAuthority("BUDGET_ITEM:MANAGE")) {
            throw new ForbiddenException("Cette action demande BUDGET_ITEM:MANAGE");
        }
        AccBudgetItem item = item(code);
        switch (decision) {
            case "approve" -> item.setStatus("ACTIVE");
            case "reject" -> item.setStatus("REJECTED");
            case "close" -> item.setStatus("CLOSED");
            case "reopen" -> item.setStatus("ACTIVE");
            case "merge" -> {
                AccBudgetItem target = item(into);
                item.setStatus("MERGED");
                item.setMergedInto(target.getCode());
                mappingRepository.findByItemCode(item.getCode()).forEach(m -> {
                    m.setItemCode(target.getCode());
                    mappingRepository.save(m);
                });
            }
            default -> throw new BadRequestException("Action inconnue : " + decision);
        }
        itemRepository.save(item);
        approvedBy(item);
        return view(item);
    }

    private void approvedBy(AccBudgetItem item) {
        item.setApprovedBy(actors.resolve("SYSTEM"));
        itemRepository.save(item);
    }

    private Item view(AccBudgetItem item) {
        List<AccBudgetItemMapping> mappings = mappingRepository.findByItemCode(item.getCode());
        return new Item(item.getCode(), item.getLabel(), item.getParentCode(), item.getItemType(), item.getResponsible(), item.getControlLevel(),
                item.getTolerancePct(), item.getControlAtParent(), item.getStatus(), item.getMergedInto(),
                values(mappings, "ACCOUNT"), values(mappings, "EXPENSE_ITEM"), values(mappings, "NATURE"));
    }

    private static List<String> values(List<AccBudgetItemMapping> mappings, String kind) {
        return mappings.stream().filter(m -> m.getKind().equals(kind)).map(AccBudgetItemMapping::getValue).toList();
    }

    // ------------------------------------------------------------------ budgets

    @Transactional(readOnly = true)
    public List<Budget> budgets(String fiscalYearCode) {
        return budgetRepository.findByFiscalYearCodeOrderByVersionDesc(fiscalYearCode).stream().map(budget -> view(budget, false)).toList();
    }

    @Transactional(readOnly = true)
    public Budget budget(Long id) {
        return view(find(id), true);
    }

    /**
     * Nouvelle version : vide ({@code EMPTY}), copie de la version active ({@code ACTIVE}, une
     * revision), ou depuis le realise de l'exercice precedent ({@code PREVIOUS_ACTUAL}) avec un
     * pourcentage d'evolution.
     */
    public Budget create(CreateBudget input) {
        prepare();
        fiscalYearRepository.findByCode(input.fiscalYearCode()).orElseThrow(() -> new ResourceNotFoundException("Exercice inconnu : " + input.fiscalYearCode()));
        int version = budgetRepository.findByFiscalYearCodeOrderByVersionDesc(input.fiscalYearCode()).stream().findFirst()
                .map(b -> b.getVersion() + 1).orElse(1);
        AccBudget budget = budgetRepository.save(AccBudget.builder().fiscalYearCode(input.fiscalYearCode()).version(version)
                .label(StringUtils.hasText(input.label()) ? input.label().trim() : version == 1 ? "Budget initial" : "Revision " + (version - 1))
                .reason(input.reason()).status("DRAFT").createdBy(actors.resolve("SYSTEM")).build());
        String basis = input.basis() == null ? "EMPTY" : input.basis().toUpperCase(Locale.ROOT);
        BigDecimal factor = BigDecimal.ONE.add((input.evolutionPct() == null ? BigDecimal.ZERO : input.evolutionPct())
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        if ("ACTIVE".equals(basis)) {
            AccBudget active = budgetRepository.findByFiscalYearCodeAndStatus(input.fiscalYearCode(), "ACTIVE")
                    .orElseThrow(() -> new BadRequestException("Aucun budget actif a reviser"));
            if (!StringUtils.hasText(input.reason())) {
                throw new BadRequestException("Une revision indique son motif");
            }
            for (AccBudgetLine line : lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(active.getId())) {
                List<BigDecimal> months = monthRepository.findByLineIdOrderByMonthAsc(line.getId()).stream().map(AccBudgetLineMonth::getAmount).toList();
                saveLine(budget, new LineInput(line.getItemCode(), line.getSectionCode(), line.getAnnualAmount(), null, months,
                        line.getControlLevel(), line.getTolerancePct(), line.getResponsible()));
            }
        } else if ("PREVIOUS_ACTUAL".equals(basis)) {
            String previous = String.valueOf(Integer.parseInt(input.fiscalYearCode()) - 1);
            for (AccBudgetItem item : itemRepository.findAllByOrderByCodeAsc()) {
                if (!"ACTIVE".equals(item.getStatus()) || mappingRepository.findByItemCode(item.getCode()).isEmpty()) {
                    continue;
                }
                AccBudgetLine probe = AccBudgetLine.builder().budgetId(budget.getId()).itemCode(item.getCode()).annualAmount(BigDecimal.ZERO).build();
                BigDecimal actual = previousActual(probe, previous);
                if (actual.signum() > 0) {
                    saveLine(budget, new LineInput(item.getCode(), null, actual.multiply(factor).setScale(0, RoundingMode.HALF_UP), "EQUAL", null,
                            null, null, null));
                }
            }
        }
        return view(budget, true);
    }

    private BigDecimal previousActual(AccBudgetLine probe, String year) {
        return engine.realized(probe, year, null);
    }

    public Line upsertLine(Long budgetId, LineInput input) {
        AccBudget budget = find(budgetId);
        if (!Set.of("DRAFT", "SUBMITTED").contains(budget.getStatus())) {
            throw new BadRequestException("Un budget consolide ou actif ne se modifie plus : creez une revision");
        }
        prepare();
        return saveLine(budget, input);
    }

    Line saveLine(AccBudget budget, LineInput input) {
        AccBudgetItem item = item(input.itemCode());
        String section = StringUtils.hasText(input.sectionCode()) ? input.sectionCode().trim().toUpperCase(Locale.ROOT) : null;
        AccBudgetLine line = lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(budget.getId()).stream()
                .filter(existing -> existing.getItemCode().equals(item.getCode()) && Objects.equals(existing.getSectionCode(), section)).findFirst()
                .orElseGet(() -> AccBudgetLine.builder().budgetId(budget.getId()).itemCode(item.getCode()).sectionCode(section).build());
        List<BigDecimal> months;
        if (input.months() != null && input.months().size() == 12) {
            months = input.months().stream().map(BudgetEngine::scale).toList();
        } else {
            BigDecimal annual = input.annualAmount() == null ? BigDecimal.ZERO : input.annualAmount();
            months = spread(annual, input.profile());
        }
        BigDecimal annual = months.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (annual.signum() < 0) {
            throw new BadRequestException("Un montant de budget est positif");
        }
        line.setAnnualAmount(BudgetEngine.scale(annual));
        if (input.controlLevel() != null) line.setControlLevel(StringUtils.hasText(input.controlLevel())
                ? oneOf(input.controlLevel(), Set.of("BLOCKING", "ALERT", "INFO"), "Niveau de controle") : null);
        if (input.tolerancePct() != null) line.setTolerancePct(input.tolerancePct());
        if (input.responsible() != null) line.setResponsible(StringUtils.hasText(input.responsible()) ? input.responsible().trim() : null);
        lineRepository.save(line);
        monthRepository.deleteByLineId(line.getId());
        monthRepository.flush();
        for (int month = 1; month <= 12; month++) {
            monthRepository.save(AccBudgetLineMonth.builder().lineId(line.getId()).month(month).amount(months.get(month - 1)).build());
        }
        return lineView(line);
    }

    /** Mensualisation : egale, ou profil saisonnier « p1,p2,...,p12 » en poids relatifs. */
    static List<BigDecimal> spread(BigDecimal annual, String profile) {
        List<BigDecimal> weights = new ArrayList<>();
        if (StringUtils.hasText(profile) && profile.contains(",")) {
            for (String part : profile.split(",")) {
                weights.add(new BigDecimal(part.trim()));
            }
            if (weights.size() != 12) {
                throw new BadRequestException("Un profil de mensualisation a 12 poids");
            }
        } else {
            weights.addAll(Collections.nCopies(12, BigDecimal.ONE));
        }
        BigDecimal total = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> result = new ArrayList<>();
        BigDecimal left = annual;
        for (int index = 0; index < 12; index++) {
            BigDecimal part = index == 11 ? left : annual.multiply(weights.get(index)).divide(total, 0, RoundingMode.HALF_UP);
            left = left.subtract(part);
            result.add(BudgetEngine.scale(part));
        }
        return result;
    }

    public Line freeze(Long lineId, boolean frozen) {
        if (!actors.hasAuthority("BUDGET:CONSOLIDATE") && !actors.hasAuthority("BUDGET:APPROVE")) {
            throw new ForbiddenException("Geler une ligne demande BUDGET:CONSOLIDATE");
        }
        AccBudgetLine line = lineRepository.findById(lineId).orElseThrow(() -> new ResourceNotFoundException("Ligne de budget introuvable : " + lineId));
        line.setFrozen(frozen);
        return lineView(lineRepository.save(line));
    }

    /** Circuit : submit (responsables), consolidate (responsable financier), approve (direction : devient actif), reject. */
    public Budget transition(Long id, String action, String reason) {
        AccBudget budget = find(id);
        String actor = actors.resolve("SYSTEM");
        switch (action) {
            case "submit" -> {
                prepare();
                expect(budget, "DRAFT");
                budget.setStatus("SUBMITTED");
                budget.setSubmittedBy(actor);
            }
            case "consolidate" -> {
                require("BUDGET:CONSOLIDATE");
                expect(budget, "SUBMITTED");
                budget.setStatus("CONSOLIDATED");
                budget.setConsolidatedBy(actor);
            }
            case "approve" -> {
                require("BUDGET:APPROVE");
                expect(budget, "CONSOLIDATED");
                budgetRepository.findByFiscalYearCodeAndStatus(budget.getFiscalYearCode(), "ACTIVE").ifPresent(active -> {
                    active.setStatus("ARCHIVED");
                    budgetRepository.saveAndFlush(active);
                    // Les engagements ouverts suivent la nouvelle version, ligne a ligne.
                    carryCommitments(active, budget);
                });
                budget.setStatus("ACTIVE");
                budget.setApprovedBy(actor);
                budget.setApprovedAt(Instant.now());
            }
            case "reject" -> {
                require("BUDGET:APPROVE");
                if (!StringUtils.hasText(reason)) {
                    throw new BadRequestException("Indiquez le motif du rejet");
                }
                budget.setStatus("REJECTED");
                budget.setReason(reason.trim());
            }
            default -> throw new BadRequestException("Action inconnue : " + action);
        }
        return view(budgetRepository.save(budget), true);
    }

    private void carryCommitments(AccBudget from, AccBudget to) {
        Map<String, Long> target = new HashMap<>();
        lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(to.getId()).forEach(l -> target.put(l.getItemCode() + "|" + l.getSectionCode(), l.getId()));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (AccBudgetLine line : lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(from.getId())) {
            Long newLine = target.get(line.getItemCode() + "|" + line.getSectionCode());
            if (newLine != null) {
                jdbc.update("UPDATE acc_budget_commitment SET line_id = ? WHERE line_id = ? AND status = 'OPEN'", newLine, line.getId());
            }
        }
    }

    // ------------------------------------------------------------------ virements

    public AccBudgetTransfer requestTransfer(TransferInput input) {
        require("BUDGET:TRANSFER_REQUEST");
        if (input.amount() == null || input.amount().signum() <= 0 || !StringUtils.hasText(input.reason())) {
            throw new BadRequestException("Un virement a un montant positif et un motif");
        }
        AccBudgetLine from = lineRepository.findById(input.fromLineId()).orElseThrow(() -> new ResourceNotFoundException("Ligne source introuvable"));
        AccBudgetLine to = lineRepository.findById(input.toLineId()).orElseThrow(() -> new ResourceNotFoundException("Ligne cible introuvable"));
        if (!from.getBudgetId().equals(to.getBudgetId())) {
            throw new BadRequestException("Un virement se fait entre lignes du meme budget");
        }
        BudgetEngine.Amounts amounts = engine.amounts(from);
        if (amounts.available().compareTo(input.amount()) < 0) {
            throw new BadRequestException("La ligne source n'a que " + BudgetEngine.plain(amounts.available()) + " XAF disponibles");
        }
        return transferRepository.save(AccBudgetTransfer.builder().fromLineId(from.getId()).toLineId(to.getId()).amount(BudgetEngine.scale(input.amount()))
                .reason(input.reason().trim()).requestedBy(actors.resolve("SYSTEM")).build());
    }

    public AccBudgetTransfer decideTransfer(Long id, boolean approve) {
        require("BUDGET:TRANSFER_APPROVE");
        AccBudgetTransfer transfer = transferRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Virement introuvable : " + id));
        if (!"REQUESTED".equals(transfer.getStatus())) {
            throw new BadRequestException("Ce virement est deja " + transfer.getStatus());
        }
        String actor = actors.resolve("SYSTEM");
        if (CashActorResolver.samePerson(actor, transfer.getRequestedBy())) {
            throw new ForbiddenException("On n'approuve pas son propre virement de credits");
        }
        if (approve) {
            AccBudgetLine from = lineRepository.lock(transfer.getFromLineId()).orElseThrow();
            AccBudgetLine to = lineRepository.lock(transfer.getToLineId()).orElseThrow();
            if (engine.amounts(from).available().compareTo(transfer.getAmount()) < 0) {
                throw new BadRequestException("La ligne source n'a plus le disponible necessaire");
            }
            shift(from, transfer.getAmount().negate());
            shift(to, transfer.getAmount());
        }
        transfer.setStatus(approve ? "APPROVED" : "REJECTED");
        transfer.setDecidedBy(actor);
        transfer.setDecidedAt(Instant.now());
        return transferRepository.save(transfer);
    }

    /** Le virement porte sur le mois courant et les suivants, au prorata. */
    private void shift(AccBudgetLine line, BigDecimal delta) {
        line.setAnnualAmount(BudgetEngine.scale(line.getAnnualAmount().add(delta)));
        lineRepository.save(line);
        int current = YearMonth.now().getMonthValue();
        List<AccBudgetLineMonth> months = monthRepository.findByLineIdOrderByMonthAsc(line.getId()).stream().filter(m -> m.getMonth() >= current).toList();
        if (months.isEmpty()) {
            return;
        }
        BigDecimal left = delta;
        for (int index = 0; index < months.size(); index++) {
            AccBudgetLineMonth month = months.get(index);
            BigDecimal part = index == months.size() - 1 ? left : delta.divide(BigDecimal.valueOf(months.size()), 0, RoundingMode.HALF_UP);
            left = left.subtract(part);
            month.setAmount(BudgetEngine.scale(month.getAmount().add(part)));
            monthRepository.save(month);
        }
    }

    @Transactional(readOnly = true)
    public List<AccBudgetTransfer> transfers() {
        return transferRepository.findAllByOrderByCreatedAtDesc();
    }

    // ------------------------------------------------------------------ previsions

    public AccBudgetForecast saveForecast(String number, ForecastInput input) {
        require("BUDGET_FORECAST:MANAGE");
        AccBudgetForecast forecast = number == null ? AccBudgetForecast.builder().number(sequences.next("budget_forecast"))
                .createdBy(actors.resolve("SYSTEM")).build() : forecast(number);
        item(input.itemCode());
        if (!StringUtils.hasText(input.subject()) || input.amount() == null || input.amount().signum() <= 0) {
            throw new BadRequestException("Une prevision a un objet et un montant positif");
        }
        try {
            YearMonth.parse(input.plannedMonth());
        } catch (RuntimeException exception) {
            throw new BadRequestException("Mois prevu au format AAAA-MM");
        }
        forecast.setItemCode(AnalyticService.code(input.itemCode()));
        forecast.setSectionCode(StringUtils.hasText(input.sectionCode()) ? input.sectionCode().trim().toUpperCase(Locale.ROOT) : null);
        forecast.setSubject(input.subject().trim());
        forecast.setAmount(BudgetEngine.scale(input.amount()));
        forecast.setPlannedMonth(input.plannedMonth());
        forecast.setRecurrence(input.recurrence() == null ? "NONE" : oneOf(input.recurrence(), Set.of("NONE", "MONTHLY", "QUARTERLY", "YEARLY"), "Recurrence"));
        forecast.setOccurrences(input.occurrences() == null ? 1 : Math.max(1, input.occurrences()));
        forecast.setSupplier(input.supplier());
        forecast.setResponsible(input.responsible());
        forecast.setPriority(input.priority() == null ? "IMPORTANT" : oneOf(input.priority(), Set.of("ESSENTIAL", "IMPORTANT", "DESIRABLE"), "Priorite"));
        return forecastRepository.save(forecast);
    }

    public AccBudgetForecast forecastStatus(String number, String status) {
        require("BUDGET_FORECAST:MANAGE");
        AccBudgetForecast forecast = forecast(number);
        forecast.setStatus(oneOf(status, Set.of("PLANNED", "POSTPONED", "CANCELLED", "REALIZED"), "Etat"));
        return forecastRepository.save(forecast);
    }

    @Transactional(readOnly = true)
    public List<AccBudgetForecast> forecasts(String itemCode) {
        return StringUtils.hasText(itemCode) ? forecastRepository.findByItemCodeOrderByPlannedMonthAsc(itemCode.trim().toUpperCase(Locale.ROOT))
                : forecastRepository.findAll(org.springframework.data.domain.Sort.by("plannedMonth"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> unplannedReasons() {
        return new JdbcTemplate(dataSource).queryForList("SELECT code, label FROM acc_unplanned_reason WHERE active ORDER BY sort_order");
    }

    // ------------------------------------------------------------------ vues

    Budget view(AccBudget budget, boolean withLines) {
        List<AccBudgetLine> lines = lineRepository.findByBudgetIdOrderByItemCodeAscSectionCodeAsc(budget.getId());
        Map<String, String> types = new HashMap<>();
        itemRepository.findAllByOrderByCodeAsc().forEach(item -> types.put(item.getCode(), item.getItemType()));
        BigDecimal charges = lines.stream().filter(l -> !"PRODUCT".equals(types.get(l.getItemCode()))).map(AccBudgetLine::getAnnualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal products = lines.stream().filter(l -> "PRODUCT".equals(types.get(l.getItemCode()))).map(AccBudgetLine::getAnnualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Budget(budget.getId(), budget.getFiscalYearCode(), budget.getVersion(), budget.getLabel(), budget.getStatus(), budget.getReason(),
                budget.getSubmittedBy(), budget.getConsolidatedBy(), budget.getApprovedBy(), budget.getApprovedAt(), budget.getCreatedBy(),
                BudgetEngine.scale(charges), BudgetEngine.scale(products), withLines ? lines.stream().map(this::lineView).toList() : List.of());
    }

    Line lineView(AccBudgetLine line) {
        BudgetEngine.Amounts amounts = engine.amounts(line);
        BigDecimal used = amounts.committed().add(amounts.realized());
        BigDecimal pct = line.getAnnualAmount().signum() == 0 ? BigDecimal.ZERO
                : used.multiply(BigDecimal.valueOf(100)).divide(line.getAnnualAmount(), 1, RoundingMode.HALF_UP);
        return new Line(line.getId(), line.getItemCode(), itemRepository.findByCode(line.getItemCode()).map(AccBudgetItem::getLabel).orElse(null),
                line.getSectionCode(), line.getAnnualAmount(), monthRepository.findByLineIdOrderByMonthAsc(line.getId()).stream()
                .map(AccBudgetLineMonth::getAmount).toList(), engine.controlLevel(line), engine.tolerance(line), line.getResponsible(), line.getFrozen(),
                amounts.committed(), amounts.realized(), amounts.available(), pct);
    }

    private AccBudget find(Long id) {
        return budgetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Budget introuvable : " + id));
    }

    AccBudgetItem item(String code) {
        return itemRepository.findByCode(AnalyticService.code(code)).orElseThrow(() -> new ResourceNotFoundException("Poste budgetaire introuvable : " + code));
    }

    private AccBudgetForecast forecast(String number) {
        return forecastRepository.findByNumber(number).orElseThrow(() -> new ResourceNotFoundException("Prevision introuvable : " + number));
    }

    private void prepare() {
        if (!actors.hasAuthority("BUDGET:PREPARE") && !actors.hasAuthority("BUDGET:CONSOLIDATE")) {
            throw new ForbiddenException("Preparer un budget demande BUDGET:PREPARE");
        }
    }

    private void require(String authority) {
        if (!actors.hasAuthority(authority)) {
            throw new ForbiddenException("Cette action demande " + authority);
        }
    }

    private static void expect(AccBudget budget, String status) {
        if (!status.equals(budget.getStatus())) {
            throw new BadRequestException("Le budget est " + budget.getStatus() + " (attendu : " + status + ")");
        }
    }

    private static String oneOf(String value, Set<String> allowed, String label) {
        String upper = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(upper)) {
            throw new BadRequestException(label + " : " + String.join(", ", allowed));
        }
        return upper;
    }
}
