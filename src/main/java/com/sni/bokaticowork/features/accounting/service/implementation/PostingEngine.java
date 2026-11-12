package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.EntryOrigin;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import com.sni.bokaticowork.features.accounting.enums.PostingRuleStatus;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccJournal;
import com.sni.bokaticowork.features.accounting.model.AccPeriod;
import com.sni.bokaticowork.features.accounting.model.AccPostingEvent;
import com.sni.bokaticowork.features.accounting.model.AccPostingRule;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.repository.AccJournalRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPostingEventRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPostingRuleRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.PostingTemplate;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Moteur d'imputation (lot F2, plan section 3.2) : de l'evenement comptable a la piece.
 *
 * <ol>
 *   <li>idempotence : un evenement (type, reference) n'est enregistre et comptabilise qu'une fois ;</li>
 *   <li>schema actif pour le type, a la date de l'evenement, dont les conditions sont remplies ;</li>
 *   <li>resolution des variables (comptes, tiers, journal) ;</li>
 *   <li>periode : la premiere periode ouverte a partir de la date du fait, avec mention ;</li>
 *   <li>piece validee, ou en brouillard (journal sans validation automatique, reprise).</li>
 * </ol>
 *
 * <p>Un echec technique (schema absent, variable non resolue, donnees illisibles) met l'evenement en
 * {@code FAILED} avec un motif lisible ; il ne remonte jamais au fait metier. Une piece calculee
 * mais desequilibree ou non conforme suit la politique des pieces desequilibrees et l'evenement
 * passe en {@code UNBALANCED}. Chaque evenement est traite dans sa propre transaction.</p>
 */
@Slf4j
@Service
public class PostingEngine {

    public static final String SYSTEM_ACTOR = "SYSTEM";

    private final AccPostingEventRepository eventRepository;
    private final AccPostingRuleRepository ruleRepository;
    private final AccEntryRepository entryRepository;
    private final AccJournalRepository journalRepository;
    private final AccountingEntryService entries;
    private final AccountingPeriodService periods;
    private final AccountingChartService chart;
    private final AccountingSettings settings;
    private final PostingTemplate template;
    private final CashActorResolver actors;
    private final TransactionTemplate newTransaction;

    public PostingEngine(AccPostingEventRepository eventRepository, AccPostingRuleRepository ruleRepository,
                         AccEntryRepository entryRepository, AccJournalRepository journalRepository, AccountingEntryService entries,
                         AccountingPeriodService periods, AccountingChartService chart, AccountingSettings settings,
                         PostingTemplate template, CashActorResolver actors, PlatformTransactionManager transactionManager) {
        this.eventRepository = eventRepository;
        this.ruleRepository = ruleRepository;
        this.entryRepository = entryRepository;
        this.journalRepository = journalRepository;
        this.entries = entries;
        this.periods = periods;
        this.chart = chart;
        this.settings = settings;
        this.template = template;
        this.actors = actors;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ------------------------------------------------------------- reception

    /**
     * Enregistre un evenement s'il est nouveau ; un evenement deja connu (meme type et reference)
     * est rendu tel quel, sans rien rejouer.
     */
    public AccPostingEvent receive(String eventType, String reference, String family, LocalDate date, String payload, boolean replay) {
        if (!StringUtils.hasText(eventType) || !StringUtils.hasText(reference)) {
            throw new BadRequestException("Un evenement comptable a un type et une reference");
        }
        String type = eventType.trim().toUpperCase(Locale.ROOT);
        String ref = reference.trim();
        Optional<AccPostingEvent> existing = eventRepository.findByEventTypeAndReference(type, ref);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return newTransaction.execute(status -> eventRepository.saveAndFlush(AccPostingEvent.builder()
                    .eventType(type)
                    .reference(ref)
                    .family(StringUtils.hasText(family) ? family.trim().toUpperCase(Locale.ROOT) : null)
                    .eventDate(date == null ? LocalDate.now(AccountingClock.ZONE) : date)
                    .payload(StringUtils.hasText(payload) ? payload : "{}")
                    .replay(replay)
                    .status(PostingEventStatus.PENDING)
                    .build()));
        } catch (DataIntegrityViolationException duplicate) {
            // Deux livraisons simultanees du meme evenement : la premiere gagne.
            return eventRepository.findByEventTypeAndReference(type, ref).orElseThrow(() -> duplicate);
        }
    }

    // ------------------------------------------------------------- traitement

    /** Traite les evenements en file (comptabilite active seulement). */
    public PostingDtos.BatchResult processPending() {
        if (!settings.enabled()) {
            return new PostingDtos.BatchResult(0, 0, 0, List.of());
        }
        List<AccPostingEvent> batch = eventRepository.findByStatusOrderByEventDateAscIdAsc(PostingEventStatus.PENDING,
                PageRequest.of(0, settings.postingBatchSize()));
        int ok = 0;
        int failed = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        for (AccPostingEvent event : batch) {
            if (!settings.familyEnabled(event.getFamily())) {
                skipped++;
                continue;
            }
            AccPostingEvent result = process(event.getId());
            if (result.getStatus() == PostingEventStatus.FAILED) {
                failed++;
                errors.add(event.getEventType() + " " + event.getReference() + " : " + result.getFailureReason());
            } else {
                ok++;
            }
        }
        return new PostingDtos.BatchResult(batch.size() - skipped, ok, failed, errors);
    }

    /** Comptabilise un evenement (en file ou en echec), dans sa propre transaction. */
    public AccPostingEvent process(Long eventId) {
        return run(eventId, false);
    }

    /**
     * Regenere un evenement en echec ou en attente d'equilibrage avec les schemas et le plan du
     * moment : la piece en attente est rejetee (suivi {@code REGENERATED}) et remplacee.
     */
    public AccPostingEvent regenerate(Long eventId) {
        AccPostingEvent event = event(eventId);
        if (event.getStatus() == PostingEventStatus.POSTED) {
            throw new BadRequestException("L'evenement " + event.getReference() + " est deja comptabilise ("
                    + Optional.ofNullable(event.getEntryNumber()).orElse(event.getProvisionalNumber())
                    + ") : contre-passez la piece si elle est fausse");
        }
        return run(eventId, true);
    }

    /** Ecarte un evenement (aucune ecriture) ; une piece en attente qu'il avait produite est rejetee. */
    public AccPostingEvent ignore(Long eventId, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez pourquoi l'evenement est ecarte");
        }
        String actor = actors.resolve(SYSTEM_ACTOR);
        return newTransaction.execute(status -> {
            AccPostingEvent event = event(eventId);
            if (event.getStatus() == PostingEventStatus.POSTED) {
                throw new BadRequestException("L'evenement est comptabilise : contre-passez la piece plutot que de l'ecarter");
            }
            currentEntry(event).ifPresent(entry -> entries.discardForRegeneration(entry, actor));
            event.setStatus(PostingEventStatus.IGNORED);
            event.setIgnoredBy(actor);
            event.setIgnoredReason(reason.trim());
            return eventRepository.save(event);
        });
    }

    /** Remet en file les evenements en echec d'un type (apres activation d'un schema). */
    public int requeueFailed(String eventType) {
        return newTransaction.execute(status -> {
            List<AccPostingEvent> failed = eventRepository.findByStatusAndEventType(PostingEventStatus.FAILED, eventType);
            failed.forEach(event -> event.setStatus(PostingEventStatus.PENDING));
            eventRepository.saveAll(failed);
            return failed.size();
        });
    }

    private AccPostingEvent run(Long eventId, boolean regenerate) {
        String actor = actors.resolve(SYSTEM_ACTOR);
        try {
            return newTransaction.execute(status -> post(event(eventId), regenerate, actor));
        } catch (RuntimeException exception) {
            String reason = readable(exception);
            log.warn("Comptabilite · evenement {} en echec : {}", eventId, reason);
            return newTransaction.execute(status -> {
                AccPostingEvent event = event(eventId);
                event.setStatus(PostingEventStatus.FAILED);
                event.setFailureReason(reason);
                event.setAttempts(event.getAttempts() + 1);
                event.setLastAttemptAt(Instant.now());
                return eventRepository.save(event);
            });
        }
    }

    private AccPostingEvent post(AccPostingEvent event, boolean regenerate, String actor) {
        if (event.getStatus() == PostingEventStatus.POSTED || event.getStatus() == PostingEventStatus.IGNORED) {
            return event;
        }
        if (event.getStatus() == PostingEventStatus.UNBALANCED && !regenerate) {
            return event;
        }
        event.setAttempts(event.getAttempts() + 1);
        event.setLastAttemptAt(Instant.now());
        if (!event.getReplay() && event.getEventDate().isBefore(settings.startDate())) {
            event.setStatus(PostingEventStatus.IGNORED);
            event.setIgnoredBy(SYSTEM_ACTOR);
            event.setIgnoredReason("Fait anterieur a la date de bascule (" + settings.startDate() + ")");
            return eventRepository.save(event);
        }
        JsonNode payload = template.read(event.getPayload());
        AccPostingRule rule = ruleFor(event.getEventType(), event.getEventDate(), payload)
                .orElseThrow(() -> new PostingTemplate.PostingException("Aucun schema actif pour " + event.getEventType()
                        + " au " + event.getEventDate() + " dont les conditions sont remplies"));
        event.setRuleCode(rule.getCode());
        event.setRuleVersion(rule.getVersion());
        if (Boolean.TRUE.equals(rule.getNoEntry())) {
            event.setStatus(PostingEventStatus.IGNORED);
            event.setIgnoredBy(SYSTEM_ACTOR);
            event.setIgnoredReason("Le schema " + rule.getCode() + " ne produit pas d'ecriture pour ce fait");
            event.setFailureReason(null);
            return eventRepository.save(event);
        }
        PostingTemplate.Plan plan = template.evaluate(rule, payload, false);
        AccJournal journal = journalRepository.findByCode(plan.journalCode())
                .orElseThrow(() -> new PostingTemplate.PostingException("Journal " + plan.journalCode() + " introuvable"));

        // Periode : la premiere ouverte a partir de la date du fait.
        LocalDate date = event.getEventDate();
        String note = null;
        AccPeriod period = periods.firstOpenOnOrAfter(date);
        if (period.getStartDate().isAfter(date)) {
            note = "Periode du " + date + " fermee : piece passee au " + period.getStartDate();
            date = period.getStartDate();
        }
        // Ordre chronologique strict du journal : un fait arrive en retard prend la derniere date du journal.
        if (settings.strictChronology()) {
            String fiscalYear = periods.fiscalYearOf(period).getCode();
            Optional<LocalDate> last = entries.lastValidatedDate(journal.getCode(), fiscalYear);
            if (last.isPresent() && date.isBefore(last.get())) {
                note = (note == null ? "" : note + " ; ") + "Fait du " + event.getEventDate() + " arrive apres une piece du "
                        + last.get() + " dans " + journal.getCode() + " : piece datee du " + last.get();
                date = last.get();
            }
        }
        if (regenerate) {
            currentEntry(event).ifPresent(entry -> entries.discardForRegeneration(entry, actor));
        }
        boolean validate = Boolean.TRUE.equals(journal.getAutoValidate()) && !Boolean.TRUE.equals(event.getReplay());
        AccEntry entry = entries.post(new AccountingEntryService.AutoPost(journal.getCode(), date,
                date.equals(event.getEventDate()) ? null : event.getEventDate(), plan.label(), origin(event), event.getEventType(),
                event.getReference(), plan.lines(), SYSTEM_ACTOR, validate, event.getId()));
        event.setProvisionalNumber(entry.getProvisionalNumber());
        event.setEntryNumber(entry.getEntryNumber());
        event.setShiftedNote(note);
        if (entry.getStatus() == EntryStatus.PENDING_BALANCE) {
            event.setStatus(PostingEventStatus.UNBALANCED);
            event.setFailureReason(entry.getPendingDetail());
        } else {
            event.setStatus(PostingEventStatus.POSTED);
            event.setFailureReason(null);
            event.setPostedAt(Instant.now());
        }
        return eventRepository.save(event);
    }

    private static EntryOrigin origin(AccPostingEvent event) {
        return "CASH".equalsIgnoreCase(event.getFamily()) ? EntryOrigin.CASH : EntryOrigin.PLATFORM;
    }

    /** Piece encore modifiable produite par l'evenement (en attente ou en brouillard). */
    private Optional<AccEntry> currentEntry(AccPostingEvent event) {
        if (!StringUtils.hasText(event.getProvisionalNumber())) {
            return Optional.empty();
        }
        return entryRepository.findByProvisionalNumber(event.getProvisionalNumber())
                .filter(entry -> entry.getStatus() == EntryStatus.PENDING_BALANCE || entry.getStatus() == EntryStatus.DRAFT);
    }

    /**
     * Schema applicable : par code, la version active la plus recente en vigueur a la date ; puis le
     * premier dont les conditions sont remplies, par priorite.
     */
    public Optional<AccPostingRule> ruleFor(String eventType, LocalDate date, JsonNode payload) {
        List<AccPostingRule> active = ruleRepository.findByEventTypeAndStatusOrderByPriorityAscVersionDesc(eventType, PostingRuleStatus.ACTIVE);
        Map<String, AccPostingRule> byCode = active.stream()
                .filter(rule -> !rule.getEffectiveFrom().isAfter(date))
                .collect(Collectors.toMap(AccPostingRule::getCode, Function.identity(),
                        (first, second) -> first.getVersion() >= second.getVersion() ? first : second));
        return byCode.values().stream()
                .sorted(Comparator.comparing(AccPostingRule::getPriority).thenComparing(AccPostingRule::getCode))
                .filter(rule -> template.matches(rule, payload))
                .findFirst();
    }

    // ------------------------------------------------------------- reprise

    /**
     * Valide en bloc les brouillards produits par les evenements (reprise de l'historique, journaux
     * sans validation automatique), dans l'ordre des dates ; chaque piece dans sa transaction.
     */
    public PostingDtos.BatchResult validateDrafts(String journalCode, LocalDate to) {
        List<AccEntry> drafts = entryRepository.findByStatusAndPostingEventIdIsNotNullOrderByEntryDateAscIdAsc(EntryStatus.DRAFT).stream()
                .filter(entry -> !StringUtils.hasText(journalCode) || entry.getJournalCode().equalsIgnoreCase(journalCode.trim()))
                .filter(entry -> to == null || !entry.getEntryDate().isAfter(to))
                .toList();
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (AccEntry draft : drafts) {
            try {
                newTransaction.executeWithoutResult(status -> {
                    entries.validate(draft.getProvisionalNumber());
                    AccEntry validated = entryRepository.findById(draft.getId()).orElseThrow();
                    eventRepository.findById(draft.getPostingEventId()).ifPresent(event -> {
                        event.setEntryNumber(validated.getEntryNumber());
                        if (validated.getStatus() == EntryStatus.PENDING_BALANCE) {
                            event.setStatus(PostingEventStatus.UNBALANCED);
                            event.setFailureReason(validated.getPendingDetail());
                        }
                        eventRepository.save(event);
                    });
                });
                ok++;
            } catch (RuntimeException exception) {
                errors.add(draft.getProvisionalNumber() + " : " + readable(exception));
            }
        }
        return new PostingDtos.BatchResult(drafts.size(), ok, drafts.size() - ok, errors);
    }

    // ------------------------------------------------------------- lecture

    public AccPostingEvent event(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Evenement comptable introuvable : " + id));
    }

    public PostingDtos.Event toResponse(AccPostingEvent event) {
        return new PostingDtos.Event(event.getId(), event.getEventType(), event.getReference(), event.getFamily(), event.getEventDate(),
                event.getPayload(), event.getStatus(), event.getReplay(), event.getFailureReason(), event.getRuleCode(), event.getRuleVersion(),
                event.getEntryNumber(), event.getProvisionalNumber(), event.getShiftedNote(), event.getAttempts(), event.getLastAttemptAt(),
                event.getPostedAt(), event.getIgnoredBy(), event.getIgnoredReason(), event.getCreatedAt());
    }

    static String readable(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause
                && !(cause instanceof PostingTemplate.PostingException) && !(cause instanceof BadRequestException)) {
            cause = cause.getCause();
        }
        String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }

    /** Fuseau des dates comptables. */
    public static final class AccountingClock {
        public static final java.time.ZoneId ZONE = java.time.ZoneId.of("Africa/Brazzaville");

        private AccountingClock() {
        }
    }
}
