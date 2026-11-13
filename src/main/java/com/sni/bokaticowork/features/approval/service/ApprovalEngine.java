package com.sni.bokaticowork.features.approval.service;

import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalEventType;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.enums.ApprovalStepStatus;
import com.sni.bokaticowork.features.approval.enums.ApproverRule;
import com.sni.bokaticowork.features.approval.model.ApprovalCircuit;
import com.sni.bokaticowork.features.approval.model.ApprovalCircuitLevel;
import com.sni.bokaticowork.features.approval.model.ApprovalDelegation;
import com.sni.bokaticowork.features.approval.model.ApprovalEvent;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.model.ApprovalStep;
import com.sni.bokaticowork.features.approval.repository.ApprovalCircuitLevelRepository;
import com.sni.bokaticowork.features.approval.repository.ApprovalCircuitRepository;
import com.sni.bokaticowork.features.approval.repository.ApprovalDelegationRepository;
import com.sni.bokaticowork.features.approval.repository.ApprovalEventRepository;
import com.sni.bokaticowork.features.approval.repository.ApprovalRequestRepository;
import com.sni.bokaticowork.features.approval.repository.ApprovalStepRepository;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Moteur commun des centres d'approbation (lot B15) : facturation, caisse, et tout centre a venir.
 *
 * <p>Une demande suit les niveaux de son circuit. Peut decider d'un niveau : son decideur attendu
 * (N+1, N+2 du demandeur ou utilisateur nomme), les detenteurs du droit du niveau, tout superieur
 * hierarchique du decideur attendu, et les delegues du jour. <b>Validation en avance</b> (D11) :
 * celui qui peut decider d'un niveau superieur approuve d'un coup tous les niveaux jusqu'au sien ;
 * les niveaux sautes sont notes « approuve en avance ». On ne decide jamais de sa propre demande.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ApprovalEngine {

    public static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    public static final String MANAGE = "APPROVAL:MANAGE";
    static final String REMINDER_HOURS_KEY = "approval.reminder-hours";
    static final String ESCALATION_HOURS_KEY = "approval.escalation-hours";

    private final ApprovalRequestRepository requests;
    private final ApprovalStepRepository steps;
    private final ApprovalEventRepository events;
    private final ApprovalDelegationRepository delegations;
    private final ApprovalCircuitRepository circuits;
    private final ApprovalCircuitLevelRepository circuitLevels;
    private final ApprovalHierarchy hierarchy;
    private final ApprovalNotifier notifier;
    private final CashActorResolver actors;
    private final SequenceGeneratorFacade sequences;
    private final ObjectProvider<ApprovalSubjectHandler> handlers;

    private PlatformSettings platformSettings;

    @Autowired(required = false)
    void setPlatformSettings(PlatformSettings platformSettings) {
        this.platformSettings = platformSettings;
    }

    // ------------------------------------------------------------------ soumission

    /** Ouvre une demande ; une demande encore ouverte sur le meme objet est remplacee. */
    public ApprovalRequest submit(ApprovalDtos.Submission submission) {
        if (!StringUtils.hasText(submission.center()) || !StringUtils.hasText(submission.requestType())
                || !StringUtils.hasText(submission.subjectRef()) || !StringUtils.hasText(submission.title())) {
            throw new BadRequestException("Centre, type, objet et intitulé sont requis pour une demande d'approbation");
        }
        String center = submission.center().trim().toUpperCase(Locale.ROOT);
        String type = submission.requestType().trim().toUpperCase(Locale.ROOT);
        String requester = StringUtils.hasText(submission.requester()) ? submission.requester().trim() : actors.current();
        if (!StringUtils.hasText(requester)) {
            throw new BadRequestException("Impossible d'identifier le demandeur");
        }
        for (ApprovalRequest open : requests.findByCenterAndRequestTypeAndSubjectRefAndStatus(center, type, submission.subjectRef().trim(),
                ApprovalStatus.PENDING)) {
            close(open, ApprovalStatus.CANCELLED, requester, "Remplacée par une nouvelle soumission");
        }
        List<ApprovalDtos.LevelSpec> specs = levelsFor(center, type, submission);
        Instant now = Instant.now();
        int start = specs.isEmpty() ? 0 : Math.min(Math.max(1, submission.startLevel() == null ? 1 : submission.startLevel()), specs.size());
        ApprovalRequest request = requests.save(ApprovalRequest.builder()
                .number(sequences.next("approval_request"))
                .center(center).requestType(type)
                .subjectType(StringUtils.hasText(submission.subjectType()) ? submission.subjectType().trim().toUpperCase(Locale.ROOT) : type)
                .subjectRef(submission.subjectRef().trim())
                .title(truncate(submission.title().trim(), 500))
                .amount(submission.amount()).currency(submission.currency())
                .requester(requester).requesterName(hierarchy.nameOf(requester))
                .status(specs.isEmpty() ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING)
                .requiredLevels(specs.size()).currentLevel(start).levelSince(now)
                .detailsJson(submission.detailsJson())
                .submittedAt(now)
                .decidedAt(specs.isEmpty() ? now : null).decidedBy(specs.isEmpty() ? "SYSTEM" : null)
                .build());
        List<String> chain = hierarchy.chain(requester);
        ApprovalStep current = null;
        for (int index = 0; index < specs.size(); index++) {
            ApprovalDtos.LevelSpec spec = specs.get(index);
            int level = index + 1;
            String expected = switch (spec.rule()) {
                case MANAGER -> {
                    int depth = spec.depth() == null ? 1 : Math.max(1, spec.depth());
                    yield chain.size() >= depth ? chain.get(depth - 1) : null;
                }
                case USER -> StringUtils.hasText(spec.userLogin()) ? spec.userLogin().trim() : null;
                case PERMISSION -> null;
            };
            if (expected == null && !StringUtils.hasText(spec.permission())) {
                throw new BadRequestException("Le niveau « " + spec.label() + " » n'a ni décideur ni droit : complétez la hiérarchie de "
                        + request.getRequesterName() + " ou le circuit");
            }
            ApprovalStep step = steps.save(ApprovalStep.builder().requestId(request.getId()).levelOrder(level).label(spec.label())
                    .approverRule(spec.rule()).expectedApprover(expected).expectedApproverName(hierarchy.nameOf(expected))
                    .permission(StringUtils.hasText(spec.permission()) ? spec.permission().trim() : null)
                    .status(level < start ? ApprovalStepStatus.APPROVED : level == start ? ApprovalStepStatus.PENDING : ApprovalStepStatus.WAITING)
                    .decidedBy(level < start ? "REPRISE" : null).decidedAt(level < start ? now : null)
                    .comment(level < start ? "Validé avant la reprise dans le centre d'approbation" : null)
                    .build());
            if (level == start) {
                current = step;
            }
        }
        event(request, 0, requester, ApprovalEventType.SUBMITTED, null, null, submission.amount(), null);
        if (current != null) {
            notifier.toApprovers(request, current, path(request), false);
        }
        return request;
    }

    /** Niveaux explicites du centre, ou ceux du circuit du type requis par le montant. */
    List<ApprovalDtos.LevelSpec> levelsFor(String center, String type, ApprovalDtos.Submission submission) {
        if (submission.levels() != null && !submission.levels().isEmpty()) {
            return submission.levels();
        }
        ApprovalCircuit circuit = circuits.findByCenterAndRequestType(center, type).filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new BadRequestException("Aucun circuit d'approbation actif pour " + type + " (" + center + ")"));
        BigDecimal amount = submission.amount() == null ? BigDecimal.ZERO : submission.amount().abs();
        return circuitLevels.findByCircuitIdOrderByLevelOrderAsc(circuit.getId()).stream()
                .filter(level -> level.getMinAmount() == null || amount.compareTo(level.getMinAmount()) >= 0)
                .map(ApprovalEngine::spec).toList();
    }

    // ------------------------------------------------------------------ decisions

    /** Approbation par les routes communes : le centre de l'objet en est informe. */
    public ApprovalDtos.Outcome approve(String number, ApprovalDtos.Decision decision) {
        ApprovalRequest request = pending(number);
        ApprovalDtos.Outcome outcome = doApprove(request, decision);
        handler(request).ifPresent(handler -> handler.onApproved(request, outcome));
        return outcome;
    }

    /** Approbation demandee par le centre lui-meme (il traite le resultat). */
    public ApprovalDtos.Outcome approveSubject(String center, String type, String subjectRef, ApprovalDtos.Decision decision) {
        return doApprove(openOrFail(center, type, subjectRef), decision);
    }

    public ApprovalDtos.Outcome reject(String number, ApprovalDtos.Decision decision) {
        ApprovalRequest request = pending(number);
        ApprovalDtos.Outcome outcome = doClose(request, decision, ApprovalStatus.REJECTED);
        handler(request).ifPresent(handler -> handler.onRejected(request, outcome));
        return outcome;
    }

    public ApprovalDtos.Outcome rejectSubject(String center, String type, String subjectRef, ApprovalDtos.Decision decision) {
        return doClose(openOrFail(center, type, subjectRef), decision, ApprovalStatus.REJECTED);
    }

    public ApprovalDtos.Outcome sendBack(String number, ApprovalDtos.Decision decision) {
        ApprovalRequest request = pending(number);
        ApprovalDtos.Outcome outcome = doClose(request, decision, ApprovalStatus.RETURNED);
        handler(request).ifPresent(handler -> handler.onReturned(request, outcome));
        return outcome;
    }

    public ApprovalDtos.Outcome sendBackSubject(String center, String type, String subjectRef, ApprovalDtos.Decision decision) {
        return doClose(openOrFail(center, type, subjectRef), decision, ApprovalStatus.RETURNED);
    }

    /** Retrait de la demande ouverte sur l'objet (annulation par le demandeur ou par le centre). */
    public void cancelSubject(String center, String type, String subjectRef, String comment) {
        String actor = actors.current();
        open(center, type, subjectRef).ifPresent(request -> close(request, ApprovalStatus.CANCELLED,
                StringUtils.hasText(actor) ? actor : request.getRequester(), comment));
    }

    private ApprovalDtos.Outcome doApprove(ApprovalRequest request, ApprovalDtos.Decision decision) {
        String actor = actorRequired();
        assertNotRequester(request, actor);
        handler(request).ifPresent(handler -> handler.beforeDecision(request, actor));
        List<ApprovalStep> all = steps.findByRequestIdOrderByLevelOrderAsc(request.getId());
        ApprovalStep current = currentStep(request, all);
        Match best = null;
        for (ApprovalStep step : all) {
            if (step.getLevelOrder() >= current.getLevelOrder()) {
                Optional<Match> match = match(request, step, actor);
                if (match.isPresent()) {
                    best = match.get();
                }
            }
        }
        if (best == null) {
            throw forbidden(current);
        }
        String comment = decision == null ? null : trim(decision.comment());
        BigDecimal before = request.getAmount();
        BigDecimal after = before;
        if (decision != null && decision.reducedAmount() != null) {
            after = decision.reducedAmount();
            if (after.signum() <= 0 || (before != null && after.compareTo(before) >= 0)) {
                throw new BadRequestException("Le montant réduit est positif et inférieur au montant demandé");
            }
            if (!StringUtils.hasText(comment)) {
                throw new BadRequestException("Indiquez le motif de la réduction");
            }
        }
        Instant now = Instant.now();
        String name = hierarchy.nameOf(actor);
        ApprovalStep reached = best.step();
        for (ApprovalStep step : all) {
            int level = step.getLevelOrder();
            if (level >= current.getLevelOrder() && level < reached.getLevelOrder()) {
                String note = "Approuvé en avance par " + name + " (" + reached.getLabel() + ")";
                decide(step, ApprovalStepStatus.APPROVED_BY_HIGHER, actor, name, best.delegatedFrom(), note, now);
                event(request, level, actor, ApprovalEventType.APPROVED_BY_HIGHER, note, before, before, best.delegatedFrom());
            }
        }
        decide(reached, ApprovalStepStatus.APPROVED, actor, name, best.delegatedFrom(), comment, now);
        event(request, reached.getLevelOrder(), actor, ApprovalEventType.APPROVED, comment, before, after, best.delegatedFrom());
        request.setAmount(after);
        request.setLastComment(comment);
        boolean last = reached.getLevelOrder() >= request.getRequiredLevels();
        String path = path(request);
        if (last) {
            request.setStatus(ApprovalStatus.APPROVED);
            request.setDecidedAt(now);
            request.setDecidedBy(actor);
            requests.save(request);
            notifier.toRequester(request, "Votre demande « " + request.getTitle() + " » est approuvée.", path);
        } else {
            request.setCurrentLevel(reached.getLevelOrder() + 1);
            request.setLevelSince(now);
            request.setRemindedAt(null);
            request.setEscalatedAt(null);
            requests.save(request);
            ApprovalStep next = all.stream().filter(step -> step.getLevelOrder().equals(request.getCurrentLevel())).findFirst().orElseThrow();
            next.setStatus(ApprovalStepStatus.PENDING);
            steps.save(next);
            notifier.toApprovers(request, next, path, false);
        }
        return new ApprovalDtos.Outcome(request.getNumber(), request.getSubjectRef(), request.getStatus(), current.getLevelOrder(),
                reached.getLevelOrder(), last, actor, best.delegatedFrom(), before, after, comment);
    }

    /** Refus ou renvoi pour correction, par quiconque peut decider du niveau courant ou d'un superieur. */
    private ApprovalDtos.Outcome doClose(ApprovalRequest request, ApprovalDtos.Decision decision, ApprovalStatus outcomeStatus) {
        String actor = actorRequired();
        assertNotRequester(request, actor);
        handler(request).ifPresent(handler -> handler.beforeDecision(request, actor));
        String comment = decision == null ? null : trim(decision.comment());
        if (!StringUtils.hasText(comment)) {
            throw new BadRequestException(outcomeStatus == ApprovalStatus.REJECTED ? "Indiquez le motif du refus" : "Dites ce qu'il faut corriger");
        }
        List<ApprovalStep> all = steps.findByRequestIdOrderByLevelOrderAsc(request.getId());
        ApprovalStep current = currentStep(request, all);
        Match match = null;
        for (ApprovalStep step : all) {
            if (step.getLevelOrder() >= current.getLevelOrder() && match == null) {
                match = match(request, step, actor).orElse(null);
            }
        }
        if (match == null) {
            throw forbidden(current);
        }
        Instant now = Instant.now();
        String name = hierarchy.nameOf(actor);
        decide(current, outcomeStatus == ApprovalStatus.REJECTED ? ApprovalStepStatus.REJECTED : ApprovalStepStatus.RETURNED, actor, name,
                match.delegatedFrom(), comment, now);
        all.stream().filter(step -> step.getStatus() == ApprovalStepStatus.WAITING).forEach(step -> {
            step.setStatus(ApprovalStepStatus.CANCELLED);
            steps.save(step);
        });
        event(request, current.getLevelOrder(), actor, outcomeStatus == ApprovalStatus.REJECTED ? ApprovalEventType.REJECTED : ApprovalEventType.RETURNED,
                comment, request.getAmount(), null, match.delegatedFrom());
        request.setStatus(outcomeStatus);
        request.setDecidedAt(now);
        request.setDecidedBy(actor);
        request.setLastComment(comment);
        requests.save(request);
        notifier.toRequester(request, "Votre demande « " + request.getTitle() + " » est "
                + (outcomeStatus == ApprovalStatus.REJECTED ? "refusée : " : "renvoyée pour correction : ") + comment, path(request));
        return new ApprovalDtos.Outcome(request.getNumber(), request.getSubjectRef(), outcomeStatus, current.getLevelOrder(), current.getLevelOrder(),
                false, actor, match.delegatedFrom(), request.getAmount(), null, comment);
    }

    private void close(ApprovalRequest request, ApprovalStatus status, String actor, String comment) {
        for (ApprovalStep step : steps.findByRequestIdOrderByLevelOrderAsc(request.getId())) {
            if (step.getStatus() == ApprovalStepStatus.PENDING || step.getStatus() == ApprovalStepStatus.WAITING) {
                step.setStatus(ApprovalStepStatus.CANCELLED);
                steps.save(step);
            }
        }
        request.setStatus(status);
        request.setDecidedAt(Instant.now());
        request.setDecidedBy(actor);
        request.setLastComment(comment);
        requests.save(request);
        event(request, request.getCurrentLevel(), actor, ApprovalEventType.CANCELLED, comment, request.getAmount(), null, null);
    }

    record Match(ApprovalStep step, String delegatedFrom) {
    }

    /**
     * L'acteur peut-il decider de ce niveau ? Decideur attendu, detenteur du droit, superieur du
     * decideur attendu, ou delegue (de la personne, ou du niveau pour les delegations par niveau).
     */
    Optional<Match> match(ApprovalRequest request, ApprovalStep step, String actor) {
        if (CashActorResolver.samePerson(actor, step.getExpectedApprover())) {
            return Optional.of(new Match(step, null));
        }
        if (StringUtils.hasText(step.getPermission()) && actors.hasAuthority(step.getPermission())) {
            return Optional.of(new Match(step, null));
        }
        if (StringUtils.hasText(step.getExpectedApprover()) && hierarchy.isAbove(actor, step.getExpectedApprover())) {
            return Optional.of(new Match(step, null));
        }
        LocalDate today = LocalDate.now(ZONE);
        for (ApprovalDelegation delegation : delegations.findByCancelledAtIsNullOrderByStartsOnAsc()) {
            if (!delegation.activeOn(today) || !CashActorResolver.samePerson(delegation.getDelegate(), actor)
                    || CashActorResolver.samePerson(delegation.getDelegator(), request.getRequester())
                    || (delegation.getCenter() != null && !delegation.getCenter().equalsIgnoreCase(request.getCenter()))) {
                continue;
            }
            if (delegation.getLevelOrder() != null) {
                if (delegation.getLevelOrder().equals(step.getLevelOrder())) {
                    return Optional.of(new Match(step, delegation.getDelegator()));
                }
                continue;
            }
            if (StringUtils.hasText(step.getExpectedApprover()) && (CashActorResolver.samePerson(delegation.getDelegator(), step.getExpectedApprover())
                    || hierarchy.isAbove(delegation.getDelegator(), step.getExpectedApprover()))) {
                return Optional.of(new Match(step, delegation.getDelegator()));
            }
        }
        return Optional.empty();
    }

    /** L'utilisateur connecte peut-il decider de la demande (niveau courant ou en avance) ? */
    boolean canDecide(ApprovalRequest request, List<ApprovalStep> all, String actor) {
        if (request.getStatus() != ApprovalStatus.PENDING || !StringUtils.hasText(actor)
                || CashActorResolver.samePerson(actor, request.getRequester())) {
            return false;
        }
        return all.stream().filter(step -> step.getLevelOrder() >= request.getCurrentLevel())
                .anyMatch(step -> match(request, step, actor).isPresent());
    }

    // ------------------------------------------------------------------ lecture

    @Transactional(readOnly = true)
    public Optional<ApprovalRequest> open(String center, String type, String subjectRef) {
        return requests.findByCenterAndRequestTypeAndSubjectRefAndStatus(upper(center), upper(type), subjectRef, ApprovalStatus.PENDING).stream()
                .findFirst();
    }

    /** Mes demandes dans un centre : ou chacune en est, qui doit agir, depuis quand. */
    @Transactional(readOnly = true)
    public List<ApprovalDtos.RequestView> mine(String center) {
        String actor = actorRequired();
        return requests.findByCenterAndRequesterIgnoreCaseOrderBySubmittedAtDesc(upper(center), actor).stream()
                .map(request -> view(request, false)).toList();
    }

    /** Les demandes qui attendent l'utilisateur connecte (lui-meme, son droit, sa place dans la hierarchie, une delegation). */
    @Transactional(readOnly = true)
    public List<ApprovalDtos.RequestView> toApprove(String center) {
        String actor = actorRequired();
        return requests.findByCenterAndStatusOrderBySubmittedAtAsc(upper(center), ApprovalStatus.PENDING).stream()
                .filter(request -> canDecide(request, steps.findByRequestIdOrderByLevelOrderAsc(request.getId()), actor))
                .map(request -> view(request, false)).toList();
    }

    /** References des objets d'un type qui attendent l'utilisateur connecte. */
    @Transactional(readOnly = true)
    public List<String> subjectsToApprove(String center, String type) {
        String actor = actorRequired();
        return requests.findByCenterAndStatusOrderBySubmittedAtAsc(upper(center), ApprovalStatus.PENDING).stream()
                .filter(request -> request.getRequestType().equalsIgnoreCase(type))
                .filter(request -> canDecide(request, steps.findByRequestIdOrderByLevelOrderAsc(request.getId()), actor))
                .map(ApprovalRequest::getSubjectRef).toList();
    }

    /**
     * Historique d'un centre, filtrable. Sans le droit APPROVAL:MANAGE, l'utilisateur ne voit que les
     * demandes ou il est demandeur, decideur attendu ou auteur d'une decision.
     */
    @Transactional(readOnly = true)
    public Page<ApprovalDtos.RequestView> history(String center, String requestType, String requester, String approver, ApprovalStatus status,
                                                  LocalDate from, LocalDate to, int page, int size) {
        String actor = actorRequired();
        String viewer = actors.hasAuthority(MANAGE) ? "" : actor;
        Instant start = from == null ? Instant.EPOCH : from.atStartOfDay(ZONE).toInstant();
        Instant end = to == null ? Instant.parse("9999-12-31T00:00:00Z") : to.plusDays(1).atStartOfDay(ZONE).toInstant();
        return requests.history(upper(center), status == null ? EnumSet.allOf(ApprovalStatus.class) : EnumSet.of(status), start, end,
                        blank(upper(requestType)), blank(requester), blank(approver), viewer,
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 200)))
                .map(request -> view(request, false));
    }

    /** Une demande et sa frise complete. */
    @Transactional(readOnly = true)
    public ApprovalDtos.RequestView get(String number) {
        ApprovalRequest request = byNumber(number);
        String actor = actorRequired();
        List<ApprovalStep> all = steps.findByRequestIdOrderByLevelOrderAsc(request.getId());
        boolean concerned = CashActorResolver.samePerson(actor, request.getRequester()) || actors.hasAuthority(MANAGE)
                || all.stream().anyMatch(step -> CashActorResolver.samePerson(actor, step.getExpectedApprover())
                || CashActorResolver.samePerson(actor, step.getDecidedBy()))
                || all.stream().anyMatch(step -> match(request, step, actor).isPresent());
        if (!concerned) {
            throw new ForbiddenException("Cette demande ne vous concerne pas");
        }
        return view(request, true);
    }

    ApprovalDtos.RequestView view(ApprovalRequest request, boolean full) {
        List<ApprovalStep> all = steps.findByRequestIdOrderByLevelOrderAsc(request.getId());
        ApprovalStep current = all.stream().filter(step -> step.getLevelOrder().equals(request.getCurrentLevel())).findFirst().orElse(null);
        String waitingFor = request.getStatus() != ApprovalStatus.PENDING || current == null ? null
                : current.getExpectedApproverName() != null ? current.getExpectedApproverName()
                : current.getPermission() != null ? "Titulaire du droit " + current.getPermission() : null;
        String actor = actors.current();
        return new ApprovalDtos.RequestView(request.getNumber(), request.getCenter(), request.getRequestType(), request.getSubjectType(),
                request.getSubjectRef(), request.getTitle(), request.getAmount(), request.getCurrency(), request.getRequester(),
                request.getRequesterName(), request.getStatus(), request.getRequiredLevels(), request.getCurrentLevel(),
                current == null ? null : current.getLabel(), waitingFor, request.getLevelSince(), request.getLastComment(),
                request.getSubmittedAt(), request.getDecidedAt(), request.getDecidedBy(), request.getDetailsJson(),
                canDecide(request, all, actor),
                all.stream().map(step -> new ApprovalDtos.StepView(step.getLevelOrder(), step.getLabel(), step.getApproverRule(),
                        step.getExpectedApprover(), step.getExpectedApproverName(), step.getPermission(), step.getStatus(), step.getDecidedBy(),
                        step.getDecidedByName(), step.getDelegatedFrom(), step.getDecidedAt(), step.getComment())).toList(),
                full ? events.findByRequestIdOrderByOccurredAtAscIdAsc(request.getId()).stream()
                        .map(event -> new ApprovalDtos.EventView(event.getLevelOrder(), event.getActor(), event.getActorName(), event.getEvent(),
                                event.getComment(), event.getAmountBefore(), event.getAmountAfter(), event.getDelegatedFrom(), event.getOccurredAt()))
                        .toList() : List.of());
    }

    // ------------------------------------------------------------------ circuits

    @Transactional(readOnly = true)
    public List<ApprovalDtos.CircuitView> circuits(String center) {
        List<ApprovalCircuit> found = StringUtils.hasText(center) ? circuits.findByCenterOrderByCodeAsc(upper(center))
                : circuits.findAllByOrderByCenterAscCodeAsc();
        return found.stream().map(this::circuitView).toList();
    }

    /** Cree ou remplace un circuit (ses niveaux sont reecrits). */
    public ApprovalDtos.CircuitView saveCircuit(ApprovalDtos.CircuitInput input) {
        String code = input.code().trim().toUpperCase(Locale.ROOT);
        String center = upper(input.center());
        String type = upper(input.requestType());
        circuits.findByCenterAndRequestType(center, type).filter(other -> !other.getCode().equals(code)).ifPresent(other -> {
            throw new BadRequestException("Le circuit " + other.getCode() + " couvre déjà " + type + " (" + center + ")");
        });
        for (ApprovalDtos.LevelSpec level : input.levels()) {
            if (level.rule() == ApproverRule.USER && !StringUtils.hasText(level.userLogin())) {
                throw new BadRequestException("Le niveau « " + level.label() + " » désigne un utilisateur : indiquez-le");
            }
            if (level.rule() == ApproverRule.PERMISSION && !StringUtils.hasText(level.permission())) {
                throw new BadRequestException("Le niveau « " + level.label() + " » se décide par un droit : indiquez-le");
            }
        }
        ApprovalCircuit circuit = circuits.findByCode(code).orElseGet(() -> ApprovalCircuit.builder().code(code).build());
        circuit.setCenter(center);
        circuit.setRequestType(type);
        circuit.setLabel(input.label().trim());
        circuit.setActive(input.active() == null || input.active());
        ApprovalCircuit saved = circuits.save(circuit);
        circuitLevels.deleteByCircuitId(saved.getId());
        circuitLevels.flush();
        int order = 1;
        for (ApprovalDtos.LevelSpec level : input.levels()) {
            circuitLevels.save(ApprovalCircuitLevel.builder().circuitId(saved.getId()).levelOrder(order++).label(level.label().trim())
                    .minAmount(level.minAmount()).approverRule(level.rule()).hierarchyDepth(level.depth())
                    .permission(trim(level.permission())).userLogin(trim(level.userLogin())).build());
        }
        return circuitView(saved);
    }

    private ApprovalDtos.CircuitView circuitView(ApprovalCircuit circuit) {
        return new ApprovalDtos.CircuitView(circuit.getCode(), circuit.getCenter(), circuit.getRequestType(), circuit.getLabel(),
                Boolean.TRUE.equals(circuit.getActive()),
                circuitLevels.findByCircuitIdOrderByLevelOrderAsc(circuit.getId()).stream().map(ApprovalEngine::spec).toList());
    }

    private static ApprovalDtos.LevelSpec spec(ApprovalCircuitLevel level) {
        return new ApprovalDtos.LevelSpec(level.getLabel(), level.getApproverRule(), level.getHierarchyDepth(), level.getPermission(),
                level.getUserLogin(), level.getMinAmount());
    }

    // ------------------------------------------------------------------ delegations

    /** Delegation de l'utilisateur connecte, datee ; par centre et par niveau, ou pour tout ce qu'il decide. */
    public ApprovalDtos.DelegationView delegate(ApprovalDtos.DelegationInput input) {
        String actor = actorRequired();
        if (CashActorResolver.samePerson(actor, input.delegate())) {
            throw new BadRequestException("On ne se délègue pas à soi-même");
        }
        if (input.endsOn().isBefore(input.startsOn())) {
            throw new BadRequestException("La délégation se termine après son début");
        }
        ApprovalDelegation delegation = delegations.save(ApprovalDelegation.builder().delegator(actor).delegate(input.delegate().trim())
                .center(StringUtils.hasText(input.center()) ? upper(input.center()) : null).levelOrder(input.level())
                .startsOn(input.startsOn()).endsOn(input.endsOn()).reason(trim(input.reason())).createdBy(actor).build());
        return delegationView(delegation);
    }

    public void cancelDelegation(Long id) {
        ApprovalDelegation delegation = delegations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Délégation introuvable : " + id));
        String actor = actors.current();
        if (actor != null && !CashActorResolver.samePerson(actor, delegation.getDelegator()) && !actors.hasAuthority(MANAGE)) {
            throw new ForbiddenException("Seul le délégant retire sa délégation");
        }
        delegation.setCancelledAt(Instant.now());
        delegations.save(delegation);
    }

    @Transactional(readOnly = true)
    public List<ApprovalDtos.DelegationView> delegations(String center) {
        return delegations.findByCancelledAtIsNullOrderByStartsOnAsc().stream()
                .filter(delegation -> !StringUtils.hasText(center) || delegation.getCenter() == null || delegation.getCenter().equalsIgnoreCase(center))
                .map(ApprovalEngine::delegationView).toList();
    }

    private static ApprovalDtos.DelegationView delegationView(ApprovalDelegation delegation) {
        return new ApprovalDtos.DelegationView(delegation.getId(), delegation.getDelegator(), delegation.getDelegate(), delegation.getCenter(),
                delegation.getLevelOrder(), delegation.getStartsOn(), delegation.getEndsOn(), delegation.getReason());
    }

    // ------------------------------------------------------------------ relances et escalade

    /** Rappel aux decideurs du niveau courant, puis escalade au superieur du decideur attendu. */
    public Map<String, Integer> remind() {
        long reminderHours = setting(REMINDER_HOURS_KEY, 24);
        long escalationHours = setting(ESCALATION_HOURS_KEY, 72);
        Instant now = Instant.now();
        int reminded = 0;
        int escalated = 0;
        for (ApprovalRequest request : requests.findByStatusOrderBySubmittedAtAsc(ApprovalStatus.PENDING)) {
            Instant since = request.getLevelSince() == null ? request.getSubmittedAt() : request.getLevelSince();
            long hours = Duration.between(since, now).toHours();
            ApprovalStep current = steps.findByRequestIdOrderByLevelOrderAsc(request.getId()).stream()
                    .filter(step -> step.getLevelOrder().equals(request.getCurrentLevel())).findFirst().orElse(null);
            if (current == null) {
                continue;
            }
            String path = path(request);
            if (hours >= reminderHours && (request.getRemindedAt() == null
                    || Duration.between(request.getRemindedAt(), now).toHours() >= reminderHours)) {
                notifier.toApprovers(request, current, path, true);
                request.setRemindedAt(now);
                event(request, current.getLevelOrder(), "SYSTEM", ApprovalEventType.REMINDED, "Rappel après " + hours + " heures", null, null, null);
                reminded++;
            }
            if (hours >= escalationHours && request.getEscalatedAt() == null) {
                List<String> above = StringUtils.hasText(current.getExpectedApprover()) ? hierarchy.chain(current.getExpectedApprover()) : List.of();
                notifier.escalate(request, current, above.isEmpty() ? null : above.get(0), hours, path);
                request.setEscalatedAt(now);
                event(request, current.getLevelOrder(), "SYSTEM", ApprovalEventType.ESCALATED,
                        "Escaladé à " + (above.isEmpty() ? "la permanence" : hierarchy.nameOf(above.get(0))), null, null, null);
                escalated++;
            }
            requests.save(request);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("reminders", reminded);
        result.put("escalations", escalated);
        return result;
    }

    // ------------------------------------------------------------------ outils

    private void decide(ApprovalStep step, ApprovalStepStatus status, String actor, String name, String delegatedFrom, String comment, Instant at) {
        step.setStatus(status);
        step.setDecidedBy(actor);
        step.setDecidedByName(name);
        step.setDelegatedFrom(delegatedFrom);
        step.setDecidedAt(at);
        step.setComment(comment);
        steps.save(step);
    }

    private void event(ApprovalRequest request, Integer level, String actor, ApprovalEventType type, String comment, BigDecimal before,
                       BigDecimal after, String delegatedFrom) {
        events.save(ApprovalEvent.builder().requestId(request.getId()).levelOrder(level).actor(actor)
                .actorName("SYSTEM".equals(actor) ? "Système" : hierarchy.nameOf(actor)).event(type).comment(comment)
                .amountBefore(before).amountAfter(after).delegatedFrom(delegatedFrom).build());
    }

    private ApprovalStep currentStep(ApprovalRequest request, List<ApprovalStep> all) {
        return all.stream().filter(step -> step.getLevelOrder().equals(request.getCurrentLevel())).findFirst()
                .orElseThrow(() -> new BadRequestException("La demande " + request.getNumber() + " n'a pas de niveau en attente"));
    }

    private ForbiddenException forbidden(ApprovalStep current) {
        if (StringUtils.hasText(current.getPermission())) {
            return new ForbiddenException("Ce niveau de validation demande le droit " + current.getPermission()
                    + (current.getExpectedApproverName() != null ? " ou la décision de " + current.getExpectedApproverName() : ""));
        }
        return new ForbiddenException("Ce niveau attend la décision de "
                + (current.getExpectedApproverName() != null ? current.getExpectedApproverName() : "son décideur") + " ou d'un supérieur");
    }

    private void assertNotRequester(ApprovalRequest request, String actor) {
        if (CashActorResolver.samePerson(actor, request.getRequester())) {
            throw new ForbiddenException("On ne valide jamais sa propre demande, même avec le niveau requis");
        }
    }

    private ApprovalRequest pending(String number) {
        ApprovalRequest request = byNumber(number);
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BadRequestException("La demande " + number + " n'est plus en attente (" + request.getStatus() + ")");
        }
        return request;
    }

    private ApprovalRequest openOrFail(String center, String type, String subjectRef) {
        return open(center, type, subjectRef)
                .orElseThrow(() -> new BadRequestException("Aucune demande d'approbation en cours pour " + subjectRef));
    }

    private ApprovalRequest byNumber(String number) {
        if (!StringUtils.hasText(number)) {
            throw new BadRequestException("Numéro de demande manquant");
        }
        return requests.findByNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Demande d'approbation introuvable : " + number));
    }

    private Optional<ApprovalSubjectHandler> handler(ApprovalRequest request) {
        return handlers.orderedStream().filter(handler -> handler.requestType().equalsIgnoreCase(request.getRequestType())).findFirst();
    }

    private String path(ApprovalRequest request) {
        return handler(request).map(handler -> handler.actionPath(request)).orElse("/approvals/" + request.getNumber());
    }

    private String actorRequired() {
        String actor = actors.current();
        if (!StringUtils.hasText(actor)) {
            throw new ForbiddenException("Une décision d'approbation exige un utilisateur connecté");
        }
        return actor;
    }

    private long setting(String key, long fallback) {
        return platformSettings == null ? fallback : platformSettings.number(key, fallback);
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String blank(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
