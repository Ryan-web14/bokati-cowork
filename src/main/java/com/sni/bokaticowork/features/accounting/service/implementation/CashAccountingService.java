package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.JournalType;
import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import com.sni.bokaticowork.features.accounting.enums.TreasuryType;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccJournal;
import com.sni.bokaticowork.features.accounting.model.AccJournalRegister;
import com.sni.bokaticowork.features.accounting.model.AccPostingEvent;
import com.sni.bokaticowork.features.accounting.repository.AccEntryLineRepository;
import com.sni.bokaticowork.features.accounting.repository.AccJournalRegisterRepository;
import com.sni.bokaticowork.features.accounting.repository.AccJournalRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.PostingOutcomeListener;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.enums.CashVarianceDecision;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.model.CashVarianceReview;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.repository.CashVarianceReviewRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.features.payment.service.support.CashFlowRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Caisse comptable (lot F3, plan sections 6 et 7) : chaque mouvement confirme, chaque ecart constate
 * et chaque imputation d'ecart devient un evenement comptable ; le resultat revient sur le
 * mouvement ou l'ecart. Chaque caisse recoit son compte {@code 571x} et son journal {@code CA-571x}.
 *
 * <p>Les paiements et remboursements clients ne passent pas ici : l'evenement de paiement les
 * ecrit (lot F4), sinon l'encaissement serait compte deux fois.</p>
 */
@Slf4j
@Service
public class CashAccountingService implements PostingOutcomeListener {

    public static final String MOVEMENT_EVENT = "CASH_MOVEMENT";
    public static final String VARIANCE_RECORDED = "CASH_VARIANCE_RECORDED";
    public static final String VARIANCE_DECIDED = "CASH_VARIANCE_DECIDED";
    public static final String VARIANCE_CANCELLED = "CASH_VARIANCE_CANCELLED";
    public static final String FAMILY = "CASH";

    static final Set<CashMovementType> WITHOUT_ENTRY = EnumSet.of(CashMovementType.PAYMENT, CashMovementType.REFUND,
            CashMovementType.CLOSING_COUNT, CashMovementType.EXCHANGE, CashMovementType.VARIANCE);
    private static final Set<String> ALL_VARIANCE_EVENTS = Set.of(VARIANCE_RECORDED, VARIANCE_DECIDED, VARIANCE_CANCELLED);

    private final CashMovementRepository movementRepository;
    private final CashVarianceReviewRepository reviewRepository;
    private final CashRegisterRepository registerRepository;
    private final CashSessionRepository sessionRepository;
    private final CashOperationNatureRepository natureRepository;
    private final AccJournalRepository journalRepository;
    private final AccJournalRegisterRepository journalRegisterRepository;
    private final AccEntryLineRepository lineRepository;
    private final AccountingChartService chart;
    private final AccountingSettings settings;
    private final PostingEngine engine;
    private final CashActorResolver actors;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate newTransaction;

    public CashAccountingService(CashMovementRepository movementRepository, CashVarianceReviewRepository reviewRepository,
                                 CashRegisterRepository registerRepository, CashSessionRepository sessionRepository,
                                 CashOperationNatureRepository natureRepository, AccJournalRepository journalRepository,
                                 AccJournalRegisterRepository journalRegisterRepository, AccEntryLineRepository lineRepository,
                                 AccountingChartService chart, AccountingSettings settings, @Lazy PostingEngine engine,
                                 CashActorResolver actors, ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.movementRepository = movementRepository;
        this.reviewRepository = reviewRepository;
        this.registerRepository = registerRepository;
        this.sessionRepository = sessionRepository;
        this.natureRepository = natureRepository;
        this.journalRepository = journalRepository;
        this.journalRegisterRepository = journalRegisterRepository;
        this.lineRepository = lineRepository;
        this.chart = chart;
        this.settings = settings;
        this.engine = engine;
        this.actors = actors;
        this.objectMapper = objectMapper;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ------------------------------------------------------------------ alimentation

    /** Transmet a la comptabilite les mouvements et ecarts en attente (comptabilite active seulement). */
    public PostingDtos.BatchResult feed() {
        if (!settings.enabled() || !settings.familyEnabled(FAMILY)) {
            return new PostingDtos.BatchResult(0, 0, 0, List.of());
        }
        int batch = settings.postingBatchSize();
        int processed = 0;
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (CashMovement movement : movementRepository.findByAccountingStatusAndStatusNotOrderByCreatedAtAscIdAsc("PENDING", CashMovementStatus.PENDING, PageRequest.of(0, batch))) {
            processed++;
            try {
                newTransaction.executeWithoutResult(status -> feedMovement(movementRepository.findById(movement.getId()).orElseThrow()));
                ok++;
            } catch (RuntimeException exception) {
                errors.add(movement.getMovementNumber() + " : " + PostingEngine.readable(exception));
                markMovement(movement.getId(), "FAILED", null, PostingEngine.readable(exception));
            }
        }
        for (CashVarianceReview review : reviewRepository.findByAccountingStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, batch))) {
            processed++;
            try {
                newTransaction.executeWithoutResult(status -> feedRecorded(reviewRepository.findById(review.getId()).orElseThrow()));
                ok++;
            } catch (RuntimeException exception) {
                errors.add(review.getReviewNumber() + " : " + PostingEngine.readable(exception));
            }
        }
        for (CashVarianceReview review : reviewRepository.decisionsToPost(PageRequest.of(0, batch))) {
            processed++;
            try {
                newTransaction.executeWithoutResult(status -> feedDecision(reviewRepository.findById(review.getId()).orElseThrow()));
                ok++;
            } catch (RuntimeException exception) {
                errors.add(review.getReviewNumber() + " : " + PostingEngine.readable(exception));
            }
        }
        for (CashVarianceReview review : reviewRepository.supersededAfterPosting()) {
            processed++;
            try {
                newTransaction.executeWithoutResult(status -> feedCancelled(reviewRepository.findById(review.getId()).orElseThrow()));
                ok++;
            } catch (RuntimeException exception) {
                errors.add(review.getReviewNumber() + " : " + PostingEngine.readable(exception));
            }
        }
        return new PostingDtos.BatchResult(processed, ok, processed - ok, errors);
    }

    void feedMovement(CashMovement movement) {
        if (WITHOUT_ENTRY.contains(movement.getMovementType()) || movement.getStatus() == CashMovementStatus.CANCELLED) {
            movement.setAccountingStatus("NOT_APPLICABLE");
            movementRepository.save(movement);
            return;
        }
        if (movement.getStatus() == CashMovementStatus.PENDING) {
            // En attente de visa : il sera transmis une fois confirme.
            return;
        }
        CashSession session = movement.getCashSession();
        CashRegister register = session.getCashRegister();
        RegisterBooks books = ensureBooks(register);
        Map<String, Object> data = new LinkedHashMap<>();
        CashFlowDirection direction = CashFlowRules.direction(movement.getMovementType(), movement.getDirection());
        BigDecimal amount = money(movement.getAmount());
        BigDecimal vat = movement.getVatAmount() == null ? BigDecimal.ZERO : money(movement.getVatAmount());
        data.put("movementNumber", movement.getMovementNumber());
        data.put("type", movement.getMovementType().name());
        data.put("direction", direction == null ? null : direction.name());
        data.put("amount", amount);
        data.put("signedAmount", direction == CashFlowDirection.OUT ? amount.negate() : amount);
        data.put("netAmount", amount.subtract(vat));
        data.put("vatAmount", vat);
        data.put("vatCode", movement.getVatCode());
        data.put("vatAccount", direction == CashFlowDirection.OUT ? settings.text("accounting.cash.vat-recoverable-account", "4452")
                : settings.text("accounting.cash.vat-collected-account", "4431"));
        data.put("registerCode", register.getRegisterCode());
        data.put("registerName", register.getName());
        data.put("sessionNumber", session.getSessionNumber());
        data.put("treasuryAccount", books.treasuryAccount());
        data.put("journalCode", journalFor(movement, register, books));
        data.put("label", label(movement));
        data.put("counterpartyCode", movement.getCounterpartyCode());
        data.put("counterpartyName", movement.getCounterpartyName());
        data.put("supplierCode", movement.getSupplierCode());
        data.put("requestNumber", movement.getRequestNumber());
        data.put("transferOrderNumber", movement.getTransferOrderNumber());
        data.put("transferReference", StringUtils.hasText(movement.getTransferOrderNumber()) ? movement.getTransferOrderNumber() : movement.getMovementNumber());
        data.put("natureCode", movement.getNatureCode());
        Optional<CashOperationNature> nature = StringUtils.hasText(movement.getNatureCode())
                ? natureRepository.findByCode(movement.getNatureCode()) : Optional.empty();
        String natureAccount = nature.map(CashOperationNature::getAccountNumber).filter(StringUtils::hasText)
                .orElse(settings.text(direction == CashFlowDirection.OUT ? "accounting.cash.default-out-account" : "accounting.cash.default-in-account", "471"));
        data.put("natureAccount", natureAccount);
        data.put("counterAccount", natureAccount);
        data.put("natureSection", nature.map(CashOperationNature::getDefaultSection).orElse(null));
        boolean thirdPartyAccount = chart.find(natureAccount).map(account -> account.getThirdPartyType() != null).orElse(false);
        String party = StringUtils.hasText(movement.getSupplierCode()) ? movement.getSupplierCode()
                : StringUtils.hasText(movement.getCounterpartyCode()) ? movement.getCounterpartyCode() : movement.getBeneficiaryName();
        data.put("natureThirdParty", thirdPartyAccount ? party : null);
        boolean first = movement.getMovementType() == CashMovementType.OPENING_FLOAT
                && !"CASH_HANDOVER".equals(movement.getReferenceType())
                && session.getOpenedAt() != null
                && !sessionRepository.existsByCashRegister_IdAndOpenedAtBefore(register.getId(), session.getOpenedAt());
        data.put("firstSession", String.valueOf(first));
        if ("REVERSAL".equals(movement.getFlowCategory()) && "CASH_MOVEMENT".equals(movement.getReferenceType())
                && StringUtils.hasText(movement.getReferenceCode())) {
            data.put("_reverse", Map.of("eventType", MOVEMENT_EVENT, "reference", movement.getReferenceCode(),
                    "reason", movement.getReversalReason() == null ? "Contre-passation de caisse" : movement.getReversalReason()));
        }
        Instant at = movement.getApprovedAt() != null ? movement.getApprovedAt() : movement.getCreatedAt();
        engine.receive(MOVEMENT_EVENT, movement.getMovementNumber(), FAMILY, date(at), json(data), replay(at));
        movement.setAccountingStatus("QUEUED");
        movement.setAccountingNote(null);
        movementRepository.save(movement);
    }

    void feedRecorded(CashVarianceReview review) {
        if (review.getStatus() == com.sni.bokaticowork.features.payment.enums.CashVarianceStatus.DECIDED && review.getDecision() == null) {
            review.setAccountingStatus("NOT_APPLICABLE");
            reviewRepository.save(review);
            return;
        }
        if (review.getAmount() == null || review.getAmount().signum() == 0) {
            review.setAccountingStatus("NOT_APPLICABLE");
            reviewRepository.save(review);
            return;
        }
        Map<String, Object> data = varianceData(review);
        engine.receive(VARIANCE_RECORDED, review.getReviewNumber(), FAMILY, date(review.getCreatedAt()), json(data), replay(review.getCreatedAt()));
        review.setAccountingStatus("QUEUED");
        reviewRepository.save(review);
    }

    void feedDecision(CashVarianceReview review) {
        if ("PENDING".equals(review.getAccountingStatus())) {
            // Le constat passe d'abord.
            feedRecorded(review);
        }
        if ("NOT_APPLICABLE".equals(review.getAccountingStatus())) {
            review.setDecisionAccountingStatus("NOT_APPLICABLE");
            reviewRepository.save(review);
            return;
        }
        Map<String, Object> data = varianceData(review);
        String imputation = review.getDecision() == CashVarianceDecision.TOLERATED
                ? (review.getAmount().signum() < 0 ? "EXPENSE" : "INCOME") : review.getDecision().name();
        data.put("decision", review.getDecision().name());
        data.put("imputation", imputation);
        Instant at = review.getDecidedAt() == null ? review.getCreatedAt() : review.getDecidedAt();
        engine.receive(VARIANCE_DECIDED, review.getReviewNumber(), FAMILY, date(at), json(data), replay(at));
        review.setDecisionAccountingStatus("QUEUED");
        reviewRepository.save(review);
    }

    void feedCancelled(CashVarianceReview review) {
        Map<String, Object> data = varianceData(review);
        data.put("_reverse", Map.of("eventType", VARIANCE_RECORDED, "reference", review.getReviewNumber(),
                "reason", "Ecart remplace par un recomptage"));
        Instant at = review.getDecidedAt() == null ? Instant.now() : review.getDecidedAt();
        engine.receive(VARIANCE_CANCELLED, review.getReviewNumber(), FAMILY, date(at), json(data), replay(at));
        review.setAccountingStatus("IGNORED");
        reviewRepository.save(review);
    }

    private Map<String, Object> varianceData(CashVarianceReview review) {
        CashRegister register = registerRepository.findByRegisterCode(review.getRegisterCode())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + review.getRegisterCode()));
        RegisterBooks books = ensureBooks(register);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reviewNumber", review.getReviewNumber());
        data.put("kind", review.getKind() == null ? null : review.getKind().name());
        data.put("amount", money(review.getAmount()));
        data.put("registerCode", register.getRegisterCode());
        data.put("registerName", register.getName());
        data.put("sessionNumber", review.getSessionNumber());
        data.put("cashier", review.getCashier());
        data.put("treasuryAccount", books.treasuryAccount());
        data.put("journalCode", books.journalCode());
        return data;
    }

    // ------------------------------------------------------------- retour du moteur

    @Override
    public boolean supports(String eventType) {
        return MOVEMENT_EVENT.equals(eventType) || ALL_VARIANCE_EVENTS.contains(eventType);
    }

    @Override
    public void onOutcome(AccPostingEvent event) {
        String status = event.getStatus().name();
        String entry = event.getEntryNumber() != null ? event.getEntryNumber() : event.getProvisionalNumber();
        String note = event.getStatus() == PostingEventStatus.POSTED || event.getStatus() == PostingEventStatus.PENDING
                ? event.getShiftedNote()
                : event.getStatus() == PostingEventStatus.IGNORED ? event.getIgnoredReason() : event.getFailureReason();
        if (event.getStatus() == PostingEventStatus.PENDING) {
            return;
        }
        switch (event.getEventType()) {
            case MOVEMENT_EVENT -> movementRepository.findByMovementNumber(event.getReference()).ifPresent(movement -> {
                movement.setAccountingStatus(status);
                movement.setAccountingEntryNumber(entry);
                movement.setAccountingNote(truncate(note));
                movementRepository.save(movement);
            });
            case VARIANCE_RECORDED -> reviewRepository.findByReviewNumber(event.getReference()).ifPresent(review -> {
                if (!"IGNORED".equals(review.getAccountingStatus())) {
                    review.setAccountingStatus(status);
                }
                review.setEntryNumber(entry);
                reviewRepository.save(review);
            });
            case VARIANCE_DECIDED -> reviewRepository.findByReviewNumber(event.getReference()).ifPresent(review -> {
                review.setDecisionAccountingStatus(status);
                review.setDecisionEntryNumber(entry);
                reviewRepository.save(review);
            });
            default -> {
            }
        }
    }

    // ------------------------------------------------------------- comptes et journaux

    /** Compte de tresorerie et journal de la caisse. */
    public record RegisterBooks(String treasuryAccount, String journalCode) {
    }

    /**
     * Compte {@code 571x} et journal {@code CA-571x} de la caisse, crees s'ils manquent ; la fiche de
     * la caisse les retient.
     */
    public RegisterBooks ensureBooks(CashRegister register) {
        String account = register.getTreasuryAccountNumber();
        boolean changed = false;
        if (!StringUtils.hasText(account) || chart.find(account).isEmpty()) {
            String parent = settings.text("accounting.cash.treasury-parent-account", "571");
            account = chart.create(new AccountingRequests.CreateAccount(parent, null, "Caisse " + register.getName(), null, null,
                    TreasuryType.CASH, null, null, null, "Compte de la caisse " + register.getRegisterCode())).number();
            register.setTreasuryAccountNumber(account);
            changed = true;
        }
        String treasury = account;
        AccJournal journal = StringUtils.hasText(register.getDefaultJournalCode())
                ? journalRepository.findByCode(register.getDefaultJournalCode().trim().toUpperCase(Locale.ROOT)).orElse(null) : null;
        if (journal == null) {
            journal = journalRepository.findAllByOrderByCodeAsc().stream()
                    .filter(candidate -> treasury.equals(candidate.getTreasuryAccountNumber()))
                    .findFirst()
                    .orElseGet(() -> journalRepository.save(AccJournal.builder().code("CA-" + treasury).label("Caisse " + register.getName())
                            .type(JournalType.CASH).treasuryAccountNumber(treasury).autoValidate(true).createdBy("SYSTEM").build()));
            register.setDefaultJournalCode(journal.getCode());
            changed = true;
        }
        if (journal.getCashRegisterCode() == null) {
            journal.setCashRegisterCode(register.getRegisterCode());
            journalRepository.save(journal);
        }
        if (changed) {
            registerRepository.save(register);
        }
        return new RegisterBooks(treasury, journal.getCode());
    }

    /** Journal de la piece : celui choisi par la caissiere s'il est autorise pour la caisse, sinon celui de la caisse. */
    private String journalFor(CashMovement movement, CashRegister register, RegisterBooks books) {
        if (!StringUtils.hasText(movement.getJournalCode())) {
            return books.journalCode();
        }
        String chosen = movement.getJournalCode().trim().toUpperCase(Locale.ROOT);
        if (!allowed(chosen, register.getRegisterCode())) {
            throw new BadRequestException("Le journal " + chosen + " n'est pas autorise pour la caisse " + register.getRegisterCode());
        }
        return chosen;
    }

    private boolean allowed(String journalCode, String registerCode) {
        AccJournal journal = journalRepository.findByCode(journalCode).orElse(null);
        if (journal == null || !Boolean.TRUE.equals(journal.getActive())) {
            return false;
        }
        return registerCode.equals(journal.getCashRegisterCode())
                || journalRegisterRepository.findByJournalCodeAndCashRegisterCode(journalCode, registerCode).isPresent();
    }

    /** Journaux proposes a la caissiere pour une caisse. */
    public List<com.sni.bokaticowork.features.accounting.dto.AccountingResponses.Journal> journalsFor(String registerCode) {
        CashRegister register = register(registerCode);
        RegisterBooks books = newTransaction.execute(status -> ensureBooks(registerRepository.findById(register.getId()).orElseThrow()));
        List<AccJournal> journals = journalRepository.findAllByOrderByCodeAsc().stream()
                .filter(journal -> Boolean.TRUE.equals(journal.getActive()) && allowed(journal.getCode(), register.getRegisterCode()))
                .sorted((a, b) -> a.getCode().equals(books.journalCode()) ? -1 : b.getCode().equals(books.journalCode()) ? 1 : a.getCode().compareTo(b.getCode()))
                .toList();
        return journals.stream().map(periodsView()).toList();
    }

    /**
     * Nouveau journal cree depuis la caisse (droit ACCOUNTING_JOURNAL:CREATE) : code propose, type
     * caisse ou operations diverses, rattache a la caisse ; l'administrateur finance est prevenu.
     */
    public com.sni.bokaticowork.features.accounting.dto.AccountingResponses.Journal createJournalFor(String registerCode, String label,
                                                                                                 JournalType type) {
        if (!StringUtils.hasText(label)) {
            throw new BadRequestException("Donnez un libelle au journal");
        }
        JournalType kind = type == null ? JournalType.CASH : type;
        if (kind != JournalType.CASH && kind != JournalType.MISC) {
            throw new BadRequestException("Depuis une caisse, on cree un journal de caisse ou d'operations diverses");
        }
        return newTransaction.execute(status -> {
            CashRegister register = register(registerCode);
            RegisterBooks books = ensureBooks(register);
            String prefix = kind == JournalType.CASH ? "CA" : "OD";
            String code = null;
            for (int index = 1; index < 1000; index++) {
                String candidate = prefix + "-" + books.treasuryAccount() + "-" + index;
                if (journalRepository.findByCode(candidate).isEmpty()) {
                    code = candidate;
                    break;
                }
            }
            AccJournal journal = journalRepository.save(AccJournal.builder().code(code).label(label.trim()).type(kind)
                    .treasuryAccountNumber(kind == JournalType.CASH ? books.treasuryAccount() : null).autoValidate(true)
                    .cashRegisterCode(register.getRegisterCode()).createdBy(actors.resolve("SYSTEM")).build());
            return periodsView().apply(journal);
        });
    }

    /** Autorise une caisse a ecrire dans un journal existant. */
    public void authorize(String journalCode, String registerCode) {
        newTransaction.executeWithoutResult(status -> {
            AccJournal journal = journalRepository.findByCode(journalCode.trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new ResourceNotFoundException("Journal introuvable : " + journalCode));
            CashRegister register = register(registerCode);
            if (journal.getType() == JournalType.CASH && journal.getTreasuryAccountNumber() != null
                    && !journal.getTreasuryAccountNumber().equals(ensureBooks(register).treasuryAccount())) {
                throw new BadRequestException("Le journal " + journal.getCode() + " est celui d'une autre caisse (compte "
                        + journal.getTreasuryAccountNumber() + ") : une caisse n'ecrit pas dans le journal de caisse d'une autre");
            }
            if (journalRegisterRepository.findByJournalCodeAndCashRegisterCode(journal.getCode(), register.getRegisterCode()).isEmpty()) {
                journalRegisterRepository.save(AccJournalRegister.builder().journalCode(journal.getCode())
                        .cashRegisterCode(register.getRegisterCode()).createdBy(actors.resolve("SYSTEM")).build());
            }
        });
    }

    public void revoke(String journalCode, String registerCode) {
        newTransaction.executeWithoutResult(status -> journalRegisterRepository
                .findByJournalCodeAndCashRegisterCode(journalCode.trim().toUpperCase(Locale.ROOT), registerCode.trim())
                .ifPresent(journalRegisterRepository::delete));
    }

    // ------------------------------------------------------------ rapprochement

    public record RegisterReconciliation(String registerCode, String registerName, String treasuryAccount, String journalCode,
                                         BigDecimal accountingBalance, BigDecimal operationalBalance, BigDecimal difference,
                                         String operationalSource, long movementsNotPosted) {
    }

    /**
     * Solde comptable du compte de chaque caisse (pieces validees) face a son solde operationnel
     * (solde de la session en cours, a defaut dernier comptage).
     */
    public List<RegisterReconciliation> reconciliation() {
        List<RegisterReconciliation> result = new ArrayList<>();
        for (CashRegister register : registerRepository.findAll()) {
            if (Boolean.TRUE.equals(register.getSystemManaged()) || !StringUtils.hasText(register.getTreasuryAccountNumber())) {
                continue;
            }
            BigDecimal accounting = balance(register.getTreasuryAccountNumber());
            Optional<CashSession> open = sessionRepository.findBlockingSessionByCashRegisterId(register.getId());
            BigDecimal operational;
            String source;
            if (open.isPresent()) {
                operational = movementRepository.findByCashSession_IdOrderByCreatedAtAsc(open.get().getId()).stream()
                        .map(CashFlowRules::signedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                source = "Session " + open.get().getSessionNumber();
            } else {
                operational = register.getLastCountAmount() == null ? BigDecimal.ZERO : register.getLastCountAmount();
                source = "Dernier comptage";
            }
            long notPosted = movementRepository.countByCashSession_CashRegister_IdAndAccountingStatusIn(register.getId(),
                    List.of("PENDING", "QUEUED", "FAILED", "UNBALANCED"));
            result.add(new RegisterReconciliation(register.getRegisterCode(), register.getName(), register.getTreasuryAccountNumber(),
                    register.getDefaultJournalCode(), money(accounting), money(operational), money(accounting.subtract(operational)), source, notPosted));
        }
        return result;
    }

    private BigDecimal balance(String account) {
        BigDecimal value = lineRepository.balanceOf(account, List.of(EntryStatus.VALIDATED, EntryStatus.REVERSED));
        return value == null ? BigDecimal.ZERO : value;
    }

    // ------------------------------------------------------------------ outils

    private CashRegister register(String code) {
        return registerRepository.findByRegisterCode(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Caisse introuvable : " + code));
    }

    private java.util.function.Function<AccJournal, com.sni.bokaticowork.features.accounting.dto.AccountingResponses.Journal> periodsView() {
        return journal -> new com.sni.bokaticowork.features.accounting.dto.AccountingResponses.Journal(journal.getCode(), journal.getLabel(),
                journal.getType(), journal.getTreasuryAccountNumber(), journal.getNumberingReset(), journal.getAutoValidate(),
                AccountingPeriodService.split(journal.getAllowedAccounts()), journal.getCashRegisterCode(), journal.getActive());
    }

    private boolean replay(Instant at) {
        LocalDate until = settings.replayUntil();
        return until != null && at != null && date(at).isBefore(until);
    }

    private static LocalDate date(Instant at) {
        return (at == null ? Instant.now() : at).atZone(PostingEngine.AccountingClock.ZONE).toLocalDate();
    }

    private String label(CashMovement movement) {
        String base = StringUtils.hasText(movement.getReason()) ? movement.getReason() : movement.getMovementType().name();
        return truncate(base + " (" + movement.getMovementNumber() + ")");
    }

    private String json(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception exception) {
            throw new IllegalStateException("Donnees comptables illisibles : " + exception.getMessage(), exception);
        }
    }

    private void markMovement(Long id, String status, String entry, String note) {
        try {
            newTransaction.executeWithoutResult(tx -> movementRepository.findById(id).ifPresent(movement -> {
                movement.setAccountingStatus(status);
                movement.setAccountingNote(truncate(note));
                movementRepository.save(movement);
            }));
        } catch (RuntimeException exception) {
            log.warn("Comptabilite · etat comptable du mouvement {} non enregistre : {}", id, exception.getMessage());
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private static String truncate(String value) {
        return value == null ? null : value.length() > 500 ? value.substring(0, 500) : value;
    }

    static boolean openSession(CashSession session) {
        return session != null && session.getStatus() != CashSessionStatus.CLOSED;
    }
}
