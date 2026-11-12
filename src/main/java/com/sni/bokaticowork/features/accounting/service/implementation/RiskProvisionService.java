package com.sni.bokaticowork.features.accounting.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccProvisionReview;
import com.sni.bokaticowork.features.accounting.model.AccRetirementObligation;
import com.sni.bokaticowork.features.accounting.model.AccRisk;
import com.sni.bokaticowork.features.accounting.model.AccRiskProvisionMove;
import com.sni.bokaticowork.features.accounting.model.AccRiskValuation;
import com.sni.bokaticowork.features.accounting.repository.AccProvisionReviewRepository;
import com.sni.bokaticowork.features.accounting.repository.AccRetirementObligationRepository;
import com.sni.bokaticowork.features.accounting.repository.AccRiskProvisionMoveRepository;
import com.sni.bokaticowork.features.accounting.repository.AccRiskRepository;
import com.sni.bokaticowork.features.accounting.repository.AccRiskValuationRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registre des risques et provisions (lot F19, sections 8 et 9) : fiches de risque, estimations,
 * constitution et reprise (utilisee ou non), reclassement long terme vers court terme, indemnites de
 * fin de carriere, depreciation des stocks et autres actifs, revue de cloture et PV {@code RVP}.
 */
@Service
@RequiredArgsConstructor
public class RiskProvisionService {

    public static final String MANAGE = "RISK_PROVISION:MANAGE";
    public static final String REVIEW = "RISK_PROVISION:REVIEW";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    /** Comptes d'une nature : provision long terme, dotation, reprise ; court terme : 4991, 6591, 7591. */
    public record NatureAccounts(String label, String provision, String expense, String reversal, boolean asset) {
    }

    public static final Map<String, NatureAccounts> NATURES = Map.ofEntries(
            Map.entry("CLIENT_LITIGATION", new NatureAccounts("Litige avec un client ou un membre", "191", "6911", "7911", false)),
            Map.entry("EMPLOYEE_LITIGATION", new NatureAccounts("Litige avec un salarie", "191", "6911", "7911", false)),
            Map.entry("PREMISES_CLAIM", new NatureAccounts("Sinistre dans les espaces", "198", "6911", "7911", false)),
            Map.entry("GUARANTEE", new NatureAccounts("Garantie donnee aux clients", "192", "6911", "7911", false)),
            Map.entry("TAX", new NatureAccounts("Controle fiscal ou redressement", "195", "6911", "7911", false)),
            Map.entry("RETIREMENT", new NatureAccounts("Indemnites de fin de carriere", "196", "6911", "7911", false)),
            Map.entry("RESTRUCTURING", new NatureAccounts("Restructuration", "197", "6911", "7911", false)),
            Map.entry("PREMISES_RESTORATION", new NatureAccounts("Remise en etat des locaux", "198", "6911", "7911", false)),
            Map.entry("OTHER", new NatureAccounts("Autre risque ou charge", "198", "6911", "7911", false)),
            Map.entry("STOCK_IMPAIRMENT", new NatureAccounts("Depreciation des stocks", "3911", "6593", "7593", true)),
            Map.entry("STAFF_ADVANCE_IMPAIRMENT", new NatureAccounts("Depreciation d'avances au personnel", "4918", "6594", "7594", true)),
            Map.entry("DISPOSAL_RECEIVABLE_IMPAIRMENT", new NatureAccounts("Depreciation d'une creance sur cession", "4918", "6594", "7594", true)),
            Map.entry("DEPOSIT_IMPAIRMENT", new NatureAccounts("Depreciation d'un depot de garantie verse", "297", "697", "797", true)),
            Map.entry("TREASURY_IMPAIRMENT", new NatureAccounts("Depreciation de la tresorerie", "59", "697", "797", true)));

    private final AccRiskRepository risks;
    private final AccRiskValuationRepository valuations;
    private final AccRiskProvisionMoveRepository moves;
    private final AccProvisionReviewRepository reviews;
    private final AccRetirementObligationRepository retirements;
    private final ReceivableRiskService receivables;
    private final AccountingSettings settings;
    private final VoucherService vouchers;
    private final CashActorResolver actors;

    public record RiskRequest(String nature, String title, String thirdParty, String contractRef, String originRef, String description,
                              String counsel, String jurisdiction, LocalDate nextDeadline, String probability, BigDecimal estimatedAmount,
                              String estimationMethod, LocalDate outflowDate, Boolean deductible, String sectionCode, String documents) {
    }

    public record RiskView(AccRisk risk, String natureLabel, boolean contingentLiability, List<AccRiskValuation> valuations,
                           List<AccRiskProvisionMove> moves, List<String> warnings) {
    }

    @Transactional(readOnly = true)
    public List<AccRisk> list(String status) {
        require(ReceivableRiskService.READ, MANAGE);
        return StringUtils.hasText(status) ? risks.findAllByStatus(status.trim().toUpperCase(Locale.ROOT)) : risks.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public RiskView get(String code) {
        require(ReceivableRiskService.READ, MANAGE);
        return view(risk(code));
    }

    @Transactional
    public RiskView create(RiskRequest request) {
        require(MANAGE);
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Un risque a un intitule");
        }
        String nature = nature(request.nature());
        LocalDate today = today();
        AccRisk risk = AccRisk.builder().code("RSK-" + today.getYear() + "-" + String.format("%04d", risks.count() + 1)).nature(nature)
                .title(request.title().trim()).createdBy(actor()).build();
        apply(risk, request);
        risk = risks.save(risk);
        valuations.save(AccRiskValuation.builder().riskId(risk.getId()).valuationDate(today).amount(risk.getEstimatedAmount())
                .probability(risk.getProbability()).reason("Ouverture de la fiche").createdBy(actor()).build());
        return view(risk);
    }

    public record ValuationRequest(LocalDate date, BigDecimal amount, String probability, String reason, String documentRef, LocalDate outflowDate) {
    }

    /** Nouvelle estimation : montant, probabilite, echeance ; la provision s'ajuste a part (route provision). */
    @Transactional
    public RiskView valuate(String code, ValuationRequest request) {
        require(MANAGE);
        AccRisk risk = open(code);
        if (request == null || request.amount() == null || request.amount().signum() < 0) {
            throw new BadRequestException("Une estimation a un montant positif ou nul");
        }
        if (!StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Une estimation est motivee");
        }
        LocalDate date = request.date() != null ? request.date() : today();
        risk.setEstimatedAmount(money(request.amount()));
        risk.setEstimatedAt(date);
        if (StringUtils.hasText(request.probability())) {
            risk.setProbability(probability(request.probability()));
        }
        if (request.outflowDate() != null) {
            risk.setOutflowDate(request.outflowDate());
            risk.setTerm(term(request.outflowDate(), date));
        }
        risk.setUpdatedAt(Instant.now());
        risks.save(risk);
        valuations.save(AccRiskValuation.builder().riskId(risk.getId()).valuationDate(date).amount(risk.getEstimatedAmount())
                .probability(risk.getProbability()).reason(request.reason().trim()).documentRef(request.documentRef()).createdBy(actor()).build());
        return view(risk);
    }

    public record ProvisionRequest(String kind, BigDecimal amount, LocalDate date, String reason, Boolean closeRisk) {
    }

    /**
     * Provision d'un risque : {@code ADJUST} porte la provision au montant estime (dotation ou reprise
     * non utilisee) ; {@code ALLOWANCE}, {@code USED_RELEASE}, {@code UNUSED_RELEASE} d'un montant donne.
     */
    @Transactional
    public RiskView provision(String code, ProvisionRequest request) {
        require(MANAGE);
        AccRisk risk = open(code);
        if (request == null || !StringUtils.hasText(request.kind())) {
            throw new BadRequestException("Operation attendue : ADJUST, ALLOWANCE, USED_RELEASE ou UNUSED_RELEASE");
        }
        if (!StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Un mouvement de provision est motive");
        }
        LocalDate date = request.date() != null ? request.date() : today();
        String kind = request.kind().trim().toUpperCase(Locale.ROOT);
        BigDecimal current = risk.getProvisionAmount();
        switch (kind) {
            case "ADJUST" -> {
                if (!"PROBABLE".equals(risk.getProbability()) && current.signum() == 0) {
                    throw new BadRequestException("Seul un risque probable se provisionne : un risque possible reste en passif eventuel");
                }
                BigDecimal target = "PROBABLE".equals(risk.getProbability()) ? risk.getEstimatedAmount() : BigDecimal.ZERO;
                BigDecimal difference = target.subtract(current);
                if (difference.signum() > 0) {
                    move(risk, "ALLOWANCE", difference, date, request.reason());
                } else if (difference.signum() < 0) {
                    move(risk, "UNUSED_RELEASE", difference.negate(), date, request.reason());
                }
            }
            case "ALLOWANCE" -> {
                if (!"PROBABLE".equals(risk.getProbability())) {
                    throw new BadRequestException("Seul un risque probable se provisionne");
                }
                move(risk, kind, positive(request.amount()), date, request.reason());
            }
            case "USED_RELEASE", "UNUSED_RELEASE" -> {
                BigDecimal amount = request.amount() == null ? current : positive(request.amount());
                if (amount.compareTo(current) > 0) {
                    throw new BadRequestException("La reprise depasse la provision (" + VoucherService.money(current) + ")");
                }
                move(risk, kind, amount, date, request.reason());
            }
            default -> throw new BadRequestException("Operation attendue : ADJUST, ALLOWANCE, USED_RELEASE ou UNUSED_RELEASE");
        }
        if (Boolean.TRUE.equals(request.closeRisk())) {
            if (risk.getProvisionAmount().signum() > 0) {
                throw new BadRequestException("Reprenez la provision avant de clore le risque");
            }
            risk.setStatus("CLOSED");
        }
        risk.setUpdatedAt(Instant.now());
        risks.save(risk);
        return view(risk);
    }

    /** Reclassement des provisions dont l'echeance passe sous un an (propose d'office a la cloture). */
    @Transactional
    public List<String> reclassShortTerm(LocalDate date) {
        require(MANAGE);
        List<String> done = new ArrayList<>();
        for (AccRisk risk : risks.findAllByStatus("OPEN")) {
            if (risk.getProvisionAmount().signum() <= 0 || NATURES.get(risk.getNature()).asset() || !"LONG".equals(risk.getTerm())
                    || risk.getOutflowDate() == null || !"SHORT".equals(term(risk.getOutflowDate(), date))) {
                continue;
            }
            move(risk, "RECLASS", risk.getProvisionAmount(), date, "Echeance a moins d'un an");
            risks.save(risk);
            done.add(risk.getCode());
        }
        return done;
    }

    private void move(AccRisk risk, String kind, BigDecimal amount, LocalDate date, String reason) {
        if (amount.signum() <= 0) {
            return;
        }
        NatureAccounts accounts = NATURES.get(risk.getNature());
        boolean shortTerm = "SHORT".equals(risk.getTerm()) && !accounts.asset();
        String provision = risk.getProvisionAccount() != null ? risk.getProvisionAccount()
                : shortTerm ? settings.text("accounting.provision.short-term-account", "4991") : accounts.provision();
        String expense = shortTerm ? settings.text("accounting.provision.short-term-expense-account", "6591") : accounts.expense();
        String reversal = shortTerm ? settings.text("accounting.provision.short-term-reversal-account", "7591") : accounts.reversal();
        String label = (switch (kind) {
            case "ALLOWANCE" -> "Dotation ";
            case "USED_RELEASE" -> "Reprise utilisee ";
            case "UNUSED_RELEASE" -> "Reprise non utilisee ";
            default -> "Reclassement a court terme ";
        }) + risk.getCode() + " " + risk.getTitle();
        List<AccountingRequests.EntryLine> lines;
        String from = null;
        String to = null;
        switch (kind) {
            case "ALLOWANCE" -> {
                lines = List.of(ReceivableRiskService.line(expense, null, label, amount, null, risk.getCode()),
                        ReceivableRiskService.line(provision, null, label, null, amount, risk.getCode()));
                risk.setProvisionAmount(risk.getProvisionAmount().add(amount));
                risk.setProvisionAccount(provision);
            }
            case "USED_RELEASE", "UNUSED_RELEASE" -> {
                lines = List.of(ReceivableRiskService.line(provision, null, label, amount, null, risk.getCode()),
                        ReceivableRiskService.line(reversal, null, label, null, amount, risk.getCode()));
                risk.setProvisionAmount(risk.getProvisionAmount().subtract(amount));
            }
            default -> {
                to = settings.text("accounting.provision.short-term-account", "4991");
                from = provision;
                lines = List.of(ReceivableRiskService.line(from, null, label, amount, null, risk.getCode()),
                        ReceivableRiskService.line(to, null, label, null, amount, risk.getCode()));
                risk.setProvisionAccount(to);
                risk.setTerm("SHORT");
            }
        }
        AccEntry entry = receivables.book(settings.text("accounting.provision.journal", "OD"), date, label,
                "ALLOWANCE".equals(kind) ? "RISK_PROVISION_POSTED" : "RECLASS".equals(kind) ? "RISK_PROVISION_RECLASS" : "RISK_PROVISION_RELEASED",
                risk.getCode(), lines, actor());
        moves.save(AccRiskProvisionMove.builder().riskId(risk.getId()).kind(kind).moveDate(date).amount(amount)
                .fromAccount(from != null ? from : "ALLOWANCE".equals(kind) ? expense : provision)
                .toAccount(to != null ? to : "ALLOWANCE".equals(kind) ? provision : reversal).reason(reason.trim())
                .entryNumber(entry.getEntryNumber()).createdBy(actor()).build());
    }

    // ================================================================== indemnites de fin de carriere

    public record RetirementRequest(String fiscalYear, String method, String assumptions, Integer headcount, BigDecimal amount, String detail) {
    }

    /**
     * Indemnites de fin de carriere : resultat d'un calcul (interne ou actuaire) importe ; la provision
     * 196 du risque {@code RETIREMENT} est portee a ce montant.
     */
    @Transactional
    public AccRetirementObligation retirement(RetirementRequest request) {
        require(MANAGE);
        if (request == null || request.amount() == null || request.amount().signum() < 0 || !StringUtils.hasText(request.fiscalYear())) {
            throw new BadRequestException("Exercice et montant de l'engagement obligatoires");
        }
        AccRisk risk = risks.findAllByStatus("OPEN").stream().filter(item -> "RETIREMENT".equals(item.getNature())).findFirst()
                .orElseGet(() -> risks.save(AccRisk.builder().code("RSK-IFC").nature("RETIREMENT").title("Indemnites de fin de carriere")
                        .probability("PROBABLE").term("LONG").createdBy(actor()).build()));
        BigDecimal previous = risk.getProvisionAmount();
        LocalDate date = today();
        risk.setEstimatedAmount(money(request.amount()));
        risk.setEstimatedAt(date);
        risk.setProbability("PROBABLE");
        BigDecimal difference = money(request.amount()).subtract(previous);
        if (difference.signum() > 0) {
            move(risk, "ALLOWANCE", difference, date, "Indemnites de fin de carriere " + request.fiscalYear());
        } else if (difference.signum() < 0) {
            move(risk, "UNUSED_RELEASE", difference.negate(), date, "Indemnites de fin de carriere " + request.fiscalYear());
        }
        risks.save(risk);
        List<AccRiskProvisionMove> history = moves.findAllByRiskIdOrderByMoveDateAscIdAsc(risk.getId());
        return retirements.save(AccRetirementObligation.builder().fiscalYear(request.fiscalYear().trim()).computedAt(date)
                .method(StringUtils.hasText(request.method()) ? request.method().trim().toUpperCase(Locale.ROOT) : "IMPORT")
                .assumptions(request.assumptions()).headcount(request.headcount() == null ? 0 : request.headcount()).amount(money(request.amount()))
                .previous(previous).detail(request.detail())
                .entryNumber(history.isEmpty() || difference.signum() == 0 ? null : history.get(history.size() - 1).getEntryNumber())
                .createdBy(actor()).build());
    }

    // ================================================================== revue de cloture

    public record ReviewRequest(String fiscalYear, String summary) {
    }

    @Transactional
    public AccProvisionReview prepareReview(ReviewRequest request) {
        require(MANAGE);
        if (request == null || !StringUtils.hasText(request.fiscalYear())) {
            throw new BadRequestException("Exercice de la revue obligatoire");
        }
        return reviews.save(AccProvisionReview.builder().fiscalYear(request.fiscalYear().trim()).reviewDate(today())
                .summary(StringUtils.hasText(request.summary()) ? request.summary().trim() : summary()).reviewedBy(actor()).build());
    }

    /** Validation par le responsable financier, autre que le preparateur : PV RVP. */
    @Transactional
    public AccProvisionReview validateReview(Long id) {
        require(REVIEW);
        AccProvisionReview review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Revue introuvable : " + id));
        if (!"PREPARED".equals(review.getStatus())) {
            throw new BadRequestException("La revue est deja validee");
        }
        if (actor().equalsIgnoreCase(review.getReviewedBy())) {
            throw new ForbiddenException("La revue est validee par une autre personne que celle qui l'a preparee");
        }
        review.setStatus("VALIDATED");
        review.setValidatedBy(actor());
        review.setValidatedAt(Instant.now());
        review.setVoucherNumber(vouchers.issueAndPrint("RVP", "PROVISION_REVIEW", review.getFiscalYear(), null,
                "Revue des provisions " + review.getFiscalYear(), actor()).number());
        return reviews.save(review);
    }

    /** Liste de cloture des provisions : revue validee, reclassements, IFC, controles des creances. */
    @Transactional(readOnly = true)
    public List<ReceivableRiskService.Finding> closingCheck(int year) {
        List<ReceivableRiskService.Finding> findings = new ArrayList<>(receivables.closingCheck(year));
        boolean reviewed = reviews.findAllByFiscalYearOrderByCreatedAtDesc(String.valueOf(year)).stream()
                .anyMatch(review -> "VALIDATED".equals(review.getStatus()));
        if (!reviewed) {
            findings.add(new ReceivableRiskService.Finding("BLOCKING", "Revue des provisions", "Revue des provisions de " + year
                    + " non validee (PV RVP)"));
        }
        LocalDate end = LocalDate.of(year, 12, 31);
        for (AccRisk risk : risks.findAllByStatus("OPEN")) {
            if (risk.getProvisionAmount().signum() > 0 && "LONG".equals(risk.getTerm()) && risk.getOutflowDate() != null
                    && "SHORT".equals(term(risk.getOutflowDate(), end)) && !NATURES.get(risk.getNature()).asset()) {
                findings.add(new ReceivableRiskService.Finding("BLOCKING", risk.getCode(), "Provision a reclasser a court terme (echeance "
                        + risk.getOutflowDate() + ")"));
            }
        }
        boolean retirementRisk = risks.findAllByStatus("OPEN").stream().anyMatch(item -> "RETIREMENT".equals(item.getNature()));
        if (retirementRisk && retirements.findAllByOrderByCreatedAtDesc().stream().noneMatch(item -> String.valueOf(year).equals(item.getFiscalYear()))) {
            findings.add(new ReceivableRiskService.Finding("BLOCKING", "Indemnites de fin de carriere", "Engagement de " + year + " non recalcule"));
        }
        return findings;
    }

    // ================================================================== outils

    private RiskView view(AccRisk risk) {
        List<String> warnings = new ArrayList<>();
        LocalDate today = today();
        if (risk.getNextDeadline() != null && risk.getNextDeadline().isBefore(today) && "OPEN".equals(risk.getStatus())) {
            warnings.add("Prochaine echeance depassee (" + risk.getNextDeadline() + ")");
        }
        if (risk.getEstimatedAt() != null && risk.getEstimatedAt().plusMonths(12).isBefore(today)) {
            warnings.add("Estimation de plus de 12 mois : a reexaminer");
        }
        if ("PROBABLE".equals(risk.getProbability()) && risk.getProvisionAmount().compareTo(risk.getEstimatedAmount()) != 0) {
            warnings.add("Provision (" + VoucherService.money(risk.getProvisionAmount()) + ") differente de l'estimation ("
                    + VoucherService.money(risk.getEstimatedAmount()) + ")");
        }
        NatureAccounts nature = NATURES.get(risk.getNature());
        boolean contingent = "POSSIBLE".equals(risk.getProbability()) && "OPEN".equals(risk.getStatus()) && !nature.asset();
        return new RiskView(risk, nature.label(), contingent, valuations.findAllByRiskIdOrderByValuationDateAsc(risk.getId()),
                moves.findAllByRiskIdOrderByMoveDateAscIdAsc(risk.getId()), warnings);
    }

    private void apply(AccRisk risk, RiskRequest request) {
        risk.setThirdParty(blank(request.thirdParty()));
        risk.setContractRef(blank(request.contractRef()));
        risk.setOriginRef(blank(request.originRef()));
        risk.setDescription(blank(request.description()));
        risk.setCounsel(blank(request.counsel()));
        risk.setJurisdiction(blank(request.jurisdiction()));
        risk.setNextDeadline(request.nextDeadline());
        risk.setProbability(probability(request.probability()));
        risk.setEstimatedAmount(request.estimatedAmount() == null ? BigDecimal.ZERO : money(request.estimatedAmount()));
        risk.setEstimationMethod(blank(request.estimationMethod()));
        risk.setEstimatedAt(today());
        risk.setOutflowDate(request.outflowDate());
        risk.setTerm(request.outflowDate() == null ? "LONG" : term(request.outflowDate(), today()));
        risk.setDeductible(request.deductible() == null ? !"TAX".equals(risk.getNature()) : request.deductible());
        risk.setSectionCode(blank(request.sectionCode()));
        risk.setDocuments(blank(request.documents()));
    }

    private String summary() {
        StringBuilder text = new StringBuilder();
        for (AccRisk risk : risks.findAllByStatus("OPEN")) {
            text.append(risk.getCode()).append(" ").append(risk.getTitle()).append(" : ").append(risk.getProbability()).append(", estime ")
                    .append(VoucherService.money(risk.getEstimatedAmount())).append(", provision ").append(VoucherService.money(risk.getProvisionAmount()))
                    .append("\n");
        }
        return text.length() > 3900 ? text.substring(0, 3900) : text.toString();
    }

    private static String term(LocalDate outflow, LocalDate from) {
        return outflow.isAfter(from.plusYears(1)) ? "LONG" : "SHORT";
    }

    private static String nature(String value) {
        String nature = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!NATURES.containsKey(nature)) {
            throw new BadRequestException("Nature de risque inconnue : " + value + ". Natures : " + String.join(", ", NATURES.keySet()));
        }
        return nature;
    }

    private static String probability(String value) {
        String probability = value == null ? "POSSIBLE" : value.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PROBABLE", "POSSIBLE", "REMOTE").contains(probability)) {
            throw new BadRequestException("Probabilite attendue : PROBABLE, POSSIBLE ou REMOTE");
        }
        return probability;
    }

    private AccRisk risk(String code) {
        return risks.findByCode(code == null ? "" : code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Risque introuvable : " + code));
    }

    private AccRisk open(String code) {
        AccRisk risk = risk(code);
        if (!"OPEN".equals(risk.getStatus())) {
            throw new BadRequestException("Le risque " + code + " est clos");
        }
        return risk;
    }

    private static BigDecimal positive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Montant positif attendu");
        }
        return money(amount);
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(0, RoundingMode.HALF_UP);
    }

    private static String blank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    private void require(String... authorities) {
        for (String authority : authorities) {
            if (actors.hasAuthority(authority)) {
                return;
            }
        }
        throw new ForbiddenException("Action reservee : " + String.join(" ou ", authorities));
    }

    private String actor() {
        return actors.resolve("SYSTEM");
    }
}
