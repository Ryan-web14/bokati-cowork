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
    /** Evenements qui prennent les donnees relues du fait (une livraison en double d'un evenement en file ne change rien). */
    private static final java.util.EnumSet<PostingEventStatus> OPEN = java.util.EnumSet.of(PostingEventStatus.FAILED, PostingEventStatus.UNBALANCED,
            PostingEventStatus.REVERSED);
    /** Lot K1 : nombre maximal d'evenements d'une recomptabilisation en lot. */
    private static final int REPOST_BATCH_LIMIT = 200;

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
    private final org.springframework.beans.factory.ObjectProvider<com.sni.bokaticowork.features.accounting.service.support.PostingOutcomeListener> listeners;
    /** Lot K0 (M3, M6) : sources capables de relire les donnees d'un evenement depuis le fait metier. */
    private org.springframework.beans.factory.ObjectProvider<com.sni.bokaticowork.features.accounting.service.support.PostingPayloadSource> payloadSources;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setPayloadSources(org.springframework.beans.factory.ObjectProvider<com.sni.bokaticowork.features.accounting.service.support.PostingPayloadSource> payloadSources) {
        this.payloadSources = payloadSources;
    }

    public PostingEngine(AccPostingEventRepository eventRepository, AccPostingRuleRepository ruleRepository,
                         AccEntryRepository entryRepository, AccJournalRepository journalRepository, AccountingEntryService entries,
                         AccountingPeriodService periods, AccountingChartService chart, AccountingSettings settings,
                         PostingTemplate template, CashActorResolver actors, PlatformTransactionManager transactionManager,
                         org.springframework.beans.factory.ObjectProvider<com.sni.bokaticowork.features.accounting.service.support.PostingOutcomeListener> listeners) {
        this.listeners = listeners;
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
            // Lot K0 (M6) : un evenement encore ouvert (en file, en echec, en attente d'equilibrage)
            // prend les donnees relues du fait ; un evenement comptabilise ou ecarte ne change plus.
            AccPostingEvent known = existing.get();
            String fresh = StringUtils.hasText(payload) ? payload : "{}";
            if (OPEN.contains(known.getStatus()) && (!fresh.equals(known.getPayload())
                    || date != null && !date.equals(known.getEventDate()))) {
                return newTransaction.execute(status -> {
                    AccPostingEvent current = event(known.getId());
                    current.setPayload(fresh);
                    if (date != null) {
                        current.setEventDate(date);
                    }
                    current.setReplay(replay);
                    return eventRepository.save(current);
                });
            }
            return known;
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
        retryFeedFailures();
        List<AccPostingEvent> batch = eventRepository.queue(PostingEventStatus.PENDING, settings.disabledFamilyList(),
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
            throw new BadRequestException("L'événement " + event.getReference() + " est déjà comptabilisé ("
                    + Optional.ofNullable(event.getEntryNumber()).orElse(event.getProvisionalNumber())
                    + ") : utilisez « Recomptabiliser » (contre-passation, relecture des données, nouvelle pièce)");
        }
        // Lot K0 (M6) : les donnees sont d'abord relues depuis le fait metier, s'il a une source.
        String refreshError = refresh(event);
        if (refreshError != null) {
            return recordFeedFailure(event.getEventType(), event.getReference(), event.getFamily(), event.getEventDate(), refreshError);
        }
        return run(eventId, true);
    }

    /**
     * Lot K0 (M3) : un fait dont la lecture a echoue se retente tout seul, de plus en plus espace
     * (tout de suite, puis une minute par tentative deja faite, une heure au plus) : un taux de change
     * saisi apres coup ou une session rattachee suffit, sans action dans l'ecran.
     */
    private void retryFeedFailures() {
        Instant now = Instant.now();
        for (AccPostingEvent failed : eventRepository.findByStatusAndPayloadOrderByLastAttemptAtAsc(PostingEventStatus.FAILED, "{}",
                PageRequest.of(0, settings.postingBatchSize()))) {
            if (!settings.familyEnabled(failed.getFamily())) {
                continue;
            }
            int attempts = failed.getAttempts() == null ? 0 : failed.getAttempts();
            long waitMinutes = Math.min(60, Math.max(0, attempts - 1));
            if (failed.getLastAttemptAt() != null && failed.getLastAttemptAt().plusSeconds(waitMinutes * 60).isAfter(now)) {
                continue;
            }
            try {
                regenerate(failed.getId());
            } catch (RuntimeException exception) {
                log.warn("Comptabilite · relecture de {} {} impossible : {}", failed.getEventType(), failed.getReference(), readable(exception));
            }
        }
    }

    /** Relit les donnees de l'evenement depuis sa source ; rend le motif de l'echec, ou rien. */
    private String refresh(AccPostingEvent event) {
        if (payloadSources == null) {
            return null;
        }
        for (var source : payloadSources.orderedStream().toList()) {
            if (source.supports(event.getEventType())) {
                try {
                    source.rebuild(event.getEventType(), event.getReference());
                } catch (RuntimeException exception) {
                    return readable(exception);
                }
                return null;
            }
        }
        return null;
    }

    /**
     * Lot K0 (M3) : la lecture d'un fait metier a echoue (taux de change manquant, session de caisse
     * introuvable...). L'evenement est enregistre en echec avec le motif : il sort de la file des
     * faits a lire (qui ne se bloque plus) et se retente par « Regenerer » une fois la cause corrigee.
     */
    public AccPostingEvent recordFeedFailure(String eventType, String reference, String family, LocalDate date, String reason) {
        String type = eventType.trim().toUpperCase(Locale.ROOT);
        String ref = reference.trim();
        String motive = "Lecture du fait impossible : " + (reason == null ? "motif inconnu" : reason);
        try {
            return newTransaction.execute(status -> {
                AccPostingEvent event = eventRepository.findByEventTypeAndReference(type, ref).orElseGet(() -> AccPostingEvent.builder()
                        .eventType(type)
                        .reference(ref)
                        .family(StringUtils.hasText(family) ? family.trim().toUpperCase(Locale.ROOT) : null)
                        .eventDate(date == null ? LocalDate.now(AccountingClock.ZONE) : date)
                        .payload("{}")
                        .replay(false)
                        .status(PostingEventStatus.FAILED)
                        .build());
                if (event.getStatus() == PostingEventStatus.POSTED || event.getStatus() == PostingEventStatus.IGNORED) {
                    return event;
                }
                event.setStatus(PostingEventStatus.FAILED);
                event.setFailureReason(motive.length() > 2000 ? motive.substring(0, 2000) : motive);
                event.setAttempts(event.getAttempts() == null ? 1 : event.getAttempts() + 1);
                event.setLastAttemptAt(Instant.now());
                return eventRepository.saveAndFlush(event);
            });
        } catch (DataIntegrityViolationException duplicate) {
            return eventRepository.findByEventTypeAndReference(type, ref).orElseThrow(() -> duplicate);
        }
    }

    /**
     * Lot K0 (M1) : une piece d'evenement validee hors du moteur (completee a la main, validee depuis
     * la liste des pieces en attente) : l'evenement passe comptabilise et les modules sont prevenus.
     * Avant, il restait « en attente d'equilibrage » pour toujours.
     */
    @org.springframework.context.event.EventListener
    public void onEntryValidated(com.sni.bokaticowork.features.accounting.service.support.EntryValidatedEvent validated) {
        if (validated.postingEventId() == null) {
            return;
        }
        eventRepository.findById(validated.postingEventId())
                .filter(event -> event.getStatus() == PostingEventStatus.UNBALANCED || event.getStatus() == PostingEventStatus.FAILED)
                .filter(event -> validated.provisionalNumber() != null && validated.provisionalNumber().equals(event.getProvisionalNumber()))
                .ifPresent(event -> {
                    event.setStatus(PostingEventStatus.POSTED);
                    event.setEntryNumber(validated.entryNumber());
                    event.setFailureReason(null);
                    event.setPostedAt(Instant.now());
                    notifyListeners(eventRepository.save(event));
                });
    }

    /**
     * Lot K1 (S1) : la piece d'un evenement a ete contre-passee hors du moteur. L'evenement passe en
     * {@code REVERSED} (avant : il restait « comptabilise » et la regeneration etait refusee) et le
     * module source est prevenu ; il se recomptabilise ou s'ecarte.
     */
    @org.springframework.context.event.EventListener
    public void onEntryReversed(com.sni.bokaticowork.features.accounting.service.support.EntryReversedEvent reversed) {
        if (reversed.postingEventId() == null) {
            return;
        }
        eventRepository.findById(reversed.postingEventId())
                .filter(event -> event.getStatus() == PostingEventStatus.POSTED)
                .filter(event -> reversed.provisionalNumber() != null && reversed.provisionalNumber().equals(event.getProvisionalNumber()))
                .ifPresent(event -> {
                    event.setStatus(PostingEventStatus.REVERSED);
                    event.setFailureReason(truncate("Pièce " + Optional.ofNullable(event.getEntryNumber()).orElse(event.getProvisionalNumber())
                            + " contre-passée par " + reversed.reversalNumber() + (reversed.reason() == null ? "" : " (" + reversed.reason() + ")")
                            + " : recomptabilisez l'événement ou écartez-le"));
                    notifyListeners(eventRepository.save(event));
                });
    }

    /**
     * Lot K1 (S3) : la piece d'un evenement (brouillard ou en attente) a ete rejetee. L'evenement
     * repasse en echec avec le motif, au lieu de garder un numero qui ne mene plus nulle part.
     */
    @org.springframework.context.event.EventListener
    public void onEntryRejected(com.sni.bokaticowork.features.accounting.service.support.EntryRejectedEvent rejected) {
        if (rejected.postingEventId() == null) {
            return;
        }
        eventRepository.findById(rejected.postingEventId())
                .filter(event -> event.getStatus() != PostingEventStatus.IGNORED)
                .filter(event -> rejected.provisionalNumber() != null && rejected.provisionalNumber().equals(event.getProvisionalNumber()))
                .ifPresent(event -> {
                    event.setStatus(PostingEventStatus.FAILED);
                    event.setEntryNumber(null);
                    event.setFailureReason(truncate("Pièce " + rejected.provisionalNumber() + " rejetée : " + rejected.reason()
                            + ". Corrigez la source ou le schéma puis régénérez, ou écartez l'événement"));
                    notifyListeners(eventRepository.save(event));
                });
    }

    private static String truncate(String value) {
        return value == null || value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    /**
     * Lot K1 (S1, M6, decision D2) : recomptabilise un evenement comptabilise dont la piece est
     * fausse (donnee source corrigee, schema corrige). La piece est contre-passee (seconde validation
     * au-dela du seuil), les donnees sont relues a la source, une nouvelle piece est passee, liee a
     * l'ancienne. Un evenement deja contre-passe ({@code REVERSED}) est seulement regenere.
     */
    public PostingDtos.RepostResult repost(Long eventId, String reason, LocalDate date) {
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Indiquez pourquoi l'événement est recomptabilisé");
        }
        AccPostingEvent event = event(eventId);
        if (event.getStatus() == PostingEventStatus.REVERSED) {
            AccPostingEvent result = regenerate(eventId);
            return new PostingDtos.RepostResult(toResponse(result), null, false, newEntry(result), outcome(result));
        }
        if (event.getStatus() != PostingEventStatus.POSTED) {
            throw new BadRequestException("Seul un événement comptabilisé se recomptabilise ; un événement en échec ou en attente d'équilibrage se régénère");
        }
        if (template.read(event.getPayload()).path("_reverse").isObject()) {
            throw new BadRequestException("Un événement d'annulation ne se recomptabilise pas : recomptabilisez le fait d'origine");
        }
        AccEntry entry = entryRepository.findByProvisionalNumber(event.getProvisionalNumber())
                .orElseThrow(() -> new BadRequestException("Pièce de l'événement introuvable : " + event.getProvisionalNumber()));
        String actor = actors.resolve(SYSTEM_ACTOR);
        String note = "Recomptabilisation : " + reason.trim();
        if (entry.getStatus() == EntryStatus.VALIDATED) {
            var reversal = newTransaction.execute(status -> entries.reverse(entry.getEntryNumber(),
                    new com.sni.bokaticowork.features.accounting.dto.AccountingRequests.Reverse(note, date), false));
            if (reversal.status() != EntryStatus.VALIDATED) {
                // Seconde validation : la contre-passation attend un autre comptable ; une fois validee,
                // l'evenement passe en REVERSED et « Recomptabiliser » le regenere.
                return new PostingDtos.RepostResult(toResponse(event(eventId)), reversal.provisionalNumber(), true, null,
                        String.join(" ; ", reversal.messages()) + ". Une fois validée, recomptabilisez de nouveau l'événement");
            }
            AccPostingEvent result = regenerate(eventId);
            return new PostingDtos.RepostResult(toResponse(result), reversal.entryNumber(), false, newEntry(result), outcome(result));
        }
        // Piece encore en brouillard (journal sans validation automatique) ou deja contre-passee.
        newTransaction.executeWithoutResult(status -> {
            AccPostingEvent current = event(eventId);
            current.setStatus(PostingEventStatus.REVERSED);
            current.setFailureReason(truncate(note));
            eventRepository.save(current);
        });
        AccPostingEvent result = regenerate(eventId);
        return new PostingDtos.RepostResult(toResponse(result), entry.getStatus() == EntryStatus.REVERSED ? entry.getReversedByEntry() : null,
                false, newEntry(result), outcome(result));
    }

    private static String newEntry(AccPostingEvent event) {
        return event.getStatus() == PostingEventStatus.POSTED || event.getStatus() == PostingEventStatus.UNBALANCED
                ? Optional.ofNullable(event.getEntryNumber()).orElse(event.getProvisionalNumber()) : null;
    }

    private static String outcome(AccPostingEvent event) {
        return switch (event.getStatus()) {
            case POSTED -> "Nouvelle pièce " + Optional.ofNullable(event.getEntryNumber()).orElse(event.getProvisionalNumber());
            case UNBALANCED -> "Nouvelle pièce " + event.getProvisionalNumber() + " en attente d'équilibrage : " + event.getFailureReason();
            case IGNORED -> "Aucune nouvelle pièce : " + event.getIgnoredReason();
            default -> "Nouvelle pièce non passée : " + event.getFailureReason();
        };
    }

    /**
     * Lot K1 : recomptabilisation en lot apres correction d'un schema. Apercu ({@code dryRun}) : pour
     * chaque evenement comptabilise du schema (ou du type) sur la periode, la piece actuelle et celle
     * que le schema actif produirait sur les donnees enregistrees. Execution : seuls les evenements
     * dont la piece change sont recomptabilises (donnees relues a la source).
     */
    public PostingDtos.RepostBatch repostBatch(PostingDtos.RepostBatchRequest request) {
        if (!StringUtils.hasText(request.ruleCode()) && !StringUtils.hasText(request.eventType())) {
            throw new BadRequestException("Indiquez le schéma ou le type d'événement à recomptabiliser");
        }
        boolean dryRun = !Boolean.FALSE.equals(request.dryRun());
        if (!dryRun && !StringUtils.hasText(request.reason())) {
            throw new BadRequestException("Indiquez pourquoi les événements sont recomptabilisés");
        }
        List<AccPostingEvent> found = StringUtils.hasText(request.ruleCode())
                ? eventRepository.findByStatusAndRuleCode(PostingEventStatus.POSTED, request.ruleCode().trim().toUpperCase(Locale.ROOT))
                : eventRepository.findByStatusAndEventType(PostingEventStatus.POSTED, request.eventType().trim().toUpperCase(Locale.ROOT));
        List<AccPostingEvent> events = found.stream()
                .filter(event -> !StringUtils.hasText(request.ruleCode()) || request.ruleCode().trim().equalsIgnoreCase(event.getRuleCode()))
                .filter(event -> !StringUtils.hasText(request.eventType()) || request.eventType().trim().equalsIgnoreCase(event.getEventType()))
                .filter(event -> request.from() == null || !event.getEventDate().isBefore(request.from()))
                .filter(event -> request.to() == null || !event.getEventDate().isAfter(request.to()))
                .filter(event -> !"_REVERSE".equals(event.getRuleCode()))
                .sorted(Comparator.comparing(AccPostingEvent::getEventDate).thenComparing(AccPostingEvent::getId))
                .limit(REPOST_BATCH_LIMIT)
                .toList();
        List<PostingDtos.RepostPreview> previews = new ArrayList<>();
        for (AccPostingEvent event : events) {
            previews.add(previewRepost(event));
        }
        if (dryRun) {
            return new PostingDtos.RepostBatch(previews, null);
        }
        int ok = 0;
        int processed = 0;
        List<String> errors = new ArrayList<>();
        for (PostingDtos.RepostPreview preview : previews) {
            if (!Boolean.TRUE.equals(preview.changed())) {
                continue;
            }
            processed++;
            try {
                PostingDtos.RepostResult result = repost(preview.eventId(), request.reason(), null);
                if (Boolean.TRUE.equals(result.awaitingSecondValidation()) || result.event().status() == PostingEventStatus.POSTED) {
                    ok++;
                } else {
                    errors.add(preview.reference() + " : " + result.message());
                }
            } catch (RuntimeException exception) {
                errors.add(preview.reference() + " : " + readable(exception));
            }
        }
        return new PostingDtos.RepostBatch(previews, new PostingDtos.BatchResult(processed, ok, processed - ok, errors));
    }

    private PostingDtos.RepostPreview previewRepost(AccPostingEvent event) {
        List<PostingDtos.PreviewLine> before = List.of();
        try {
            String reference = Optional.ofNullable(event.getEntryNumber()).orElse(event.getProvisionalNumber());
            before = entries.get(reference).lines().stream().map(line -> new PostingDtos.PreviewLine(line.accountNumber(), line.accountLabel(),
                    true, line.thirdPartyCode(), line.label(), line.debit(), line.credit(), line.reference())).toList();
        } catch (RuntimeException missing) {
            // Piece introuvable : l'apercu montre seulement la piece qui serait produite.
        }
        try {
            JsonNode payload = template.read(event.getPayload());
            AccPostingRule rule = ruleFor(event.getEventType(), event.getEventDate(), payload)
                    .orElseThrow(() -> new PostingTemplate.PostingException("Aucun schéma actif dont les conditions sont remplies"));
            PostingTemplate.Plan plan = template.evaluate(rule, payload, false);
            List<PostingDtos.PreviewLine> after = plan.lines().stream().map(line -> new PostingDtos.PreviewLine(line.accountNumber(), null,
                    chart.find(line.accountNumber()).isPresent(), line.thirdPartyCode(), line.label(), AccountingEntryService.money(line.debit()),
                    AccountingEntryService.money(line.credit()), line.reference())).toList();
            return new PostingDtos.RepostPreview(event.getId(), event.getEventType(), event.getReference(), event.getEventDate(),
                    Optional.ofNullable(event.getEntryNumber()).orElse(event.getProvisionalNumber()), event.getRuleCode(), event.getRuleVersion(),
                    rule.getVersion(), !signature(before).equals(signature(after)), before, after, null);
        } catch (RuntimeException exception) {
            return new PostingDtos.RepostPreview(event.getId(), event.getEventType(), event.getReference(), event.getEventDate(),
                    Optional.ofNullable(event.getEntryNumber()).orElse(event.getProvisionalNumber()), event.getRuleCode(), event.getRuleVersion(),
                    null, false, before, List.of(), readable(exception));
        }
    }

    /** Lignes comparees sans ordre : compte, tiers, debit, credit. */
    private static List<String> signature(List<PostingDtos.PreviewLine> lines) {
        return lines.stream().map(line -> line.accountNumber() + "|" + Optional.ofNullable(line.thirdPartyCode()).orElse("") + "|"
                        + AccountingEntryService.plain(line.debit()) + "|" + AccountingEntryService.plain(line.credit()))
                .sorted().toList();
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
            return newTransaction.execute(status -> notifyListeners(post(event(eventId), regenerate, actor)));
        } catch (RuntimeException exception) {
            String reason = readable(exception);
            log.warn("Comptabilite · evenement {} en echec : {}", eventId, reason);
            return newTransaction.execute(status -> {
                AccPostingEvent event = event(eventId);
                event.setStatus(PostingEventStatus.FAILED);
                event.setFailureReason(reason);
                event.setAttempts(event.getAttempts() + 1);
                event.setLastAttemptAt(Instant.now());
                return notifyListeners(eventRepository.save(event));
            });
        }
    }

    private AccPostingEvent notifyListeners(AccPostingEvent event) {
        for (var listener : listeners.orderedStream().toList()) {
            if (listener.supports(event.getEventType())) {
                listener.onOutcome(event);
            }
        }
        return event;
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
        // Lot K1 : piece contre-passee hors du moteur, la nouvelle piece lui sera liee.
        String previousEntry = event.getStatus() == PostingEventStatus.REVERSED ? event.getEntryNumber() : null;
        if (!event.getReplay() && event.getEventDate().isBefore(settings.startDate())) {
            event.setStatus(PostingEventStatus.IGNORED);
            event.setIgnoredBy(SYSTEM_ACTOR);
            event.setIgnoredReason("Fait anterieur a la date de bascule (" + settings.startDate() + ")");
            return eventRepository.save(event);
        }
        JsonNode payload = template.read(event.getPayload());
        JsonNode reverse = payload.path("_reverse");
        if (reverse.isObject()) {
            return reverse(event, reverse, regenerate, actor);
        }
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
        if (plan.lines().isEmpty()) {
            // Lot K0 (A10) : fait a montant nul (facture a zero, reste a annuler nul) : rien a ecrire.
            event.setStatus(PostingEventStatus.IGNORED);
            event.setIgnoredBy(SYSTEM_ACTOR);
            event.setIgnoredReason("Montant nul : aucune ecriture a passer");
            event.setFailureReason(null);
            return eventRepository.save(event);
        }
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
        // Ordre chronologique strict du journal, limite au mois (lot K1, decision D3) : un fait arrive en
        // retard garde sa date si sa periode est ouverte ; dans son mois, il prend au plus tot la derniere
        // date du journal. Avant, il passait sur le mois suivant (chiffre d'affaires et TVA du mauvais mois).
        if (settings.strictChronology()) {
            String fiscalYear = periods.fiscalYearOf(period).getCode();
            Optional<LocalDate> last = entries.lastValidatedDate(journal.getCode(), fiscalYear, period.getCode());
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
        if (previousEntry != null) {
            entry.setCorrectionOf(previousEntry);
            entry.setCorrectionKind("REPOST");
            entryRepository.save(entry);
            String current = entry.getEntryNumber() != null ? entry.getEntryNumber() : entry.getProvisionalNumber();
            entryRepository.findByEntryNumber(previousEntry).ifPresent(old -> {
                old.setCorrectedBy(current);
                entryRepository.save(old);
            });
        }
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

    /**
     * Evenement d'annulation ({@code "_reverse": {"eventType": .., "reference": ..}}) : contre-passation
     * de la piece de l'evenement d'origine. Tant que l'origine n'est pas comptabilisee, l'evenement
     * reste en file. Origine en brouillard (reprise) : brouillard inverse.
     */
    private AccPostingEvent reverse(AccPostingEvent event, JsonNode target, boolean regenerate, String actor) {
        String type = target.path("eventType").asText("");
        String reference = target.path("reference").asText("");
        AccPostingEvent original = eventRepository.findByEventTypeAndReference(type.toUpperCase(Locale.ROOT), reference).orElse(null);
        String reason = target.path("reason").asText("Annulation " + event.getReference());
        if (original == null || original.getStatus() == PostingEventStatus.PENDING || original.getStatus() == PostingEventStatus.FAILED
                || original.getStatus() == PostingEventStatus.UNBALANCED) {
            event.setStatus(PostingEventStatus.PENDING);
            event.setFailureReason("En attente de la comptabilisation de " + type + " " + reference);
            return eventRepository.save(event);
        }
        event.setRuleCode("_REVERSE");
        event.setRuleVersion(null);
        if (original.getStatus() == PostingEventStatus.IGNORED) {
            event.setStatus(PostingEventStatus.IGNORED);
            event.setIgnoredBy(SYSTEM_ACTOR);
            event.setIgnoredReason("Le fait annule n'avait pas d'ecriture");
            event.setFailureReason(null);
            return eventRepository.save(event);
        }
        AccEntry source = entryRepository.findByProvisionalNumber(original.getProvisionalNumber())
                .orElseThrow(() -> new PostingTemplate.PostingException("Piece d'origine introuvable : " + original.getProvisionalNumber()));
        if (source.getStatus() == EntryStatus.REVERSED) {
            event.setStatus(PostingEventStatus.POSTED);
            event.setEntryNumber(source.getReversedByEntry());
            event.setFailureReason(null);
            event.setPostedAt(Instant.now());
            return eventRepository.save(event);
        }
        LocalDate date = event.getEventDate();
        AccPeriod period = periods.firstOpenOnOrAfter(date);
        if (period.getStartDate().isAfter(date)) {
            event.setShiftedNote("Periode du " + date + " fermee : piece passee au " + period.getStartDate());
            date = period.getStartDate();
        }
        if (source.getStatus() == EntryStatus.VALIDATED) {
            if (date.isBefore(source.getEntryDate())) {
                date = source.getEntryDate();
            }
            // Lot K0 (M4) : chronologie stricte, la contre-passation prend au plus tot la date de la
            // derniere piece validee de son journal (avant : refus, et l'annulation restait en echec).
            if (settings.strictChronology()) {
                AccPeriod reversalPeriod = periods.firstOpenOnOrAfter(date);
                Optional<LocalDate> last = entries.lastValidatedDate(source.getJournalCode(), periods.fiscalYearOf(reversalPeriod).getCode(),
                        reversalPeriod.getCode());
                if (last.isPresent() && date.isBefore(last.get())) {
                    event.setShiftedNote((event.getShiftedNote() == null ? "" : event.getShiftedNote() + " ; ") + "Annulation du "
                            + event.getEventDate() + " datee du " + last.get() + ", derniere piece du journal " + source.getJournalCode());
                    date = last.get();
                }
            }
            var response = entries.reverse(source.getEntryNumber(),
                    new com.sni.bokaticowork.features.accounting.dto.AccountingRequests.Reverse(reason, date), true);
            AccEntry reversal = entryRepository.findByEntryNumber(response.entryNumber()).orElseThrow();
            reversal.setPostingEventId(event.getId());
            entryRepository.save(reversal);
            event.setEntryNumber(reversal.getEntryNumber());
            event.setProvisionalNumber(reversal.getProvisionalNumber());
        } else {
            // Origine en brouillard (reprise) : brouillard inverse dans le meme journal.
            var lines = entries.swappedLines(source);
            AccEntry draft = entries.post(new AccountingEntryService.AutoPost(source.getJournalCode(), date, null,
                    "Contre-passation de " + source.getProvisionalNumber() + " : " + reason, origin(event), event.getEventType(),
                    event.getReference(), lines, SYSTEM_ACTOR, false, event.getId()));
            event.setProvisionalNumber(draft.getProvisionalNumber());
        }
        event.setStatus(PostingEventStatus.POSTED);
        event.setFailureReason(null);
        event.setPostedAt(Instant.now());
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
