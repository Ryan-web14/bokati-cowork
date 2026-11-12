package com.sni.bokaticowork.features.accounting.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import com.sni.bokaticowork.features.accounting.enums.PostingRuleStatus;
import com.sni.bokaticowork.features.accounting.model.AccAccount;
import com.sni.bokaticowork.features.accounting.model.AccPostingEvent;
import com.sni.bokaticowork.features.accounting.model.AccPostingRule;
import com.sni.bokaticowork.features.accounting.repository.AccJournalRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPostingEventRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPostingRuleRepository;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.PostingTemplate;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Schemas d'ecriture (lot F2) : edition versionnee, apercu sur un evenement, activation.
 *
 * <p>Un schema actif ne se modifie pas : l'enregistrer cree une nouvelle version en brouillon. Un
 * schema ne s'active que s'il produit, sur un evenement d'exemple (le sien ou le dernier evenement
 * recu de ce type), une piece equilibree dont le journal et les comptes existent.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PostingRuleService {

    private final AccPostingRuleRepository ruleRepository;
    private final AccPostingEventRepository eventRepository;
    private final AccJournalRepository journalRepository;
    private final AccountingChartService chart;
    private final AccountingSettings settings;
    private final PostingTemplate template;
    private final PostingEngine engine;
    private final CashActorResolver actors;

    @Transactional(readOnly = true)
    public List<PostingDtos.Rule> rules(String eventType) {
        return ruleRepository.latestVersions(StringUtils.hasText(eventType) ? eventType.trim().toUpperCase(Locale.ROOT) : null)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PostingDtos.Rule> versions(String code) {
        List<AccPostingRule> versions = ruleRepository.findByCodeOrderByVersionDesc(code.trim().toUpperCase(Locale.ROOT));
        if (versions.isEmpty()) {
            throw new ResourceNotFoundException("Schema d'ecriture introuvable : " + code);
        }
        return versions.stream().map(this::toResponse).toList();
    }

    /** Nouveau schema, ou nouvelle version d'un schema existant (le brouillon en cours est mis a jour). */
    public PostingDtos.Rule save(PostingDtos.RuleRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        String actor = actors.resolve(PostingEngine.SYSTEM_ACTOR);
        List<AccPostingRule> versions = ruleRepository.findByCodeOrderByVersionDesc(code);
        AccPostingRule rule;
        if (!versions.isEmpty() && versions.get(0).getStatus() == PostingRuleStatus.DRAFT) {
            rule = versions.get(0);
        } else {
            rule = AccPostingRule.builder()
                    .code(code)
                    .version(versions.isEmpty() ? 1 : versions.get(0).getVersion() + 1)
                    .createdBy(actor)
                    .status(PostingRuleStatus.DRAFT)
                    .build();
        }
        apply(rule, request);
        // Verifie la syntaxe tout de suite.
        template.conditions(rule.getConditions());
        template.lines(rule.getLines());
        return toResponse(ruleRepository.save(rule));
    }

    private void apply(AccPostingRule rule, PostingDtos.RuleRequest request) {
        rule.setEventType(request.eventType().trim().toUpperCase(Locale.ROOT));
        rule.setLabel(request.label().trim());
        rule.setDescription(request.description());
        rule.setFamily(request.family().trim().toUpperCase(Locale.ROOT));
        rule.setPriority(request.priority() == null ? 100 : request.priority());
        rule.setConditions(json(request.conditions()));
        rule.setJournal(StringUtils.hasText(request.journal()) ? request.journal().trim() : null);
        rule.setEntryLabel(request.entryLabel());
        rule.setLines(json(request.lines()));
        rule.setNoEntry(Boolean.TRUE.equals(request.noEntry()));
        rule.setSamplePayload(json(request.samplePayload()));
        rule.setEffectiveFrom(request.effectiveFrom() == null ? settings.startDate() : request.effectiveFrom());
        if (!rule.getNoEntry() && (!StringUtils.hasText(rule.getJournal()) || !StringUtils.hasText(rule.getLines()))) {
            throw new BadRequestException("Un schema qui produit une ecriture indique son journal et ses lignes");
        }
    }

    /**
     * Active une version : refusee si, sur l'evenement d'exemple, la piece n'est pas equilibree, si le
     * journal ou un compte manque, ou si une variable ne se resout pas. Les versions actives plus
     * anciennes du meme code, en vigueur a partir de la meme date ou apres, sont retirees ; les
     * evenements en echec de ce type sont remis en file.
     */
    public PostingDtos.Rule activate(Long id) {
        AccPostingRule rule = rule(id);
        if (rule.getStatus() == PostingRuleStatus.ACTIVE) {
            return toResponse(rule);
        }
        if (!Boolean.TRUE.equals(rule.getNoEntry())) {
            JsonNode sample = sample(rule).orElseThrow(() -> new BadRequestException("Le schema " + rule.getCode()
                    + " n'a pas d'evenement d'exemple et aucun evenement " + rule.getEventType()
                    + " n'a encore ete recu : ajoutez un exemple pour pouvoir le verifier"));
            PostingDtos.Preview preview = preview(rule, sample);
            List<String> problems = new ArrayList<>();
            if (preview.error() != null) {
                problems.add(preview.error());
            } else {
                if (!Boolean.TRUE.equals(preview.matches())) {
                    problems.add("ses conditions ne sont pas remplies par l'exemple");
                }
                if (!Boolean.TRUE.equals(preview.balanced())) {
                    problems.add("la piece est desequilibree (debit " + plain(preview.totalDebit()) + ", credit "
                            + plain(preview.totalCredit()) + ")");
                }
                if (preview.lines().size() < 2) {
                    problems.add("il produit moins de deux lignes");
                }
                if (!Boolean.TRUE.equals(preview.journalExists())) {
                    problems.add("le journal " + preview.journalCode() + " n'existe pas");
                }
                preview.lines().stream().filter(line -> !Boolean.TRUE.equals(line.accountExists()))
                        .forEach(line -> problems.add("le compte " + line.accountNumber() + " n'existe pas"));
                problems.addAll(preview.warnings());
            }
            if (!problems.isEmpty()) {
                throw new BadRequestException("Le schema " + rule.getCode() + " ne s'active pas : " + String.join(" ; ", problems));
            }
        }
        String actor = actors.resolve(PostingEngine.SYSTEM_ACTOR);
        for (AccPostingRule other : ruleRepository.findByCodeOrderByVersionDesc(rule.getCode())) {
            if (!other.getId().equals(rule.getId()) && other.getStatus() == PostingRuleStatus.ACTIVE
                    && !other.getEffectiveFrom().isBefore(rule.getEffectiveFrom())) {
                other.setStatus(PostingRuleStatus.RETIRED);
                ruleRepository.save(other);
            }
        }
        rule.setStatus(PostingRuleStatus.ACTIVE);
        rule.setActivatedBy(actor);
        rule.setActivatedAt(Instant.now());
        AccPostingRule saved = ruleRepository.saveAndFlush(rule);
        engine.requeueFailed(rule.getEventType());
        return toResponse(saved);
    }

    public PostingDtos.Rule retire(Long id) {
        AccPostingRule rule = rule(id);
        rule.setStatus(PostingRuleStatus.RETIRED);
        return toResponse(ruleRepository.save(rule));
    }

    // ---------------------------------------------------------------- apercu

    @Transactional(readOnly = true)
    public PostingDtos.Preview preview(PostingDtos.PreviewRequest request) {
        AccPostingRule rule;
        if (request.rule() != null) {
            rule = AccPostingRule.builder().code(request.rule().code()).version(0).createdBy("APERCU").build();
            apply(rule, request.rule());
        } else if (request.ruleId() != null) {
            rule = rule(request.ruleId());
        } else if (request.eventId() != null) {
            AccPostingEvent event = engine.event(request.eventId());
            JsonNode payload = template.read(event.getPayload());
            rule = engine.ruleFor(event.getEventType(), event.getEventDate(), payload)
                    .orElseThrow(() -> new BadRequestException("Aucun schema actif pour " + event.getEventType()));
        } else {
            throw new BadRequestException("Indiquez un schema (enregistre ou en cours d'edition) ou un evenement");
        }
        JsonNode payload;
        if (request.payload() != null) {
            payload = template.read(json(request.payload()));
        } else if (request.eventId() != null) {
            payload = template.read(engine.event(request.eventId()).getPayload());
        } else {
            payload = sample(rule).orElseThrow(() -> new BadRequestException("Fournissez un evenement d'exemple"));
        }
        return preview(rule, payload);
    }

    PostingDtos.Preview preview(AccPostingRule rule, JsonNode payload) {
        boolean matches;
        try {
            matches = template.matches(rule, payload);
        } catch (RuntimeException exception) {
            return error(rule, exception);
        }
        if (Boolean.TRUE.equals(rule.getNoEntry())) {
            return new PostingDtos.Preview(rule.getCode(), rule.getVersion(), matches, null, null, rule.getLabel(), List.of(),
                    BigDecimal.ZERO, BigDecimal.ZERO, true, List.of("Ce schema ne produit pas d'ecriture"), null);
        }
        PostingTemplate.Plan plan;
        try {
            plan = template.evaluate(rule, payload, true);
        } catch (RuntimeException exception) {
            return error(rule, exception);
        }
        List<PostingDtos.PreviewLine> lines = plan.lines().stream().map(line -> {
            Optional<AccAccount> account = chart.find(line.accountNumber());
            return new PostingDtos.PreviewLine(line.accountNumber(), account.map(AccAccount::getLabel).orElse(null), account.isPresent(),
                    line.thirdPartyCode(), line.label(), line.debit(), line.credit(), line.reference());
        }).toList();
        return new PostingDtos.Preview(rule.getCode(), rule.getVersion(), matches, plan.journalCode(),
                journalRepository.findByCode(plan.journalCode()).isPresent(), plan.label(), lines, plan.totalDebit(), plan.totalCredit(),
                plan.balanced(), plan.warnings(), null);
    }

    private static PostingDtos.Preview error(AccPostingRule rule, RuntimeException exception) {
        return new PostingDtos.Preview(rule.getCode(), rule.getVersion(), false, null, false, rule.getLabel(), List.of(), BigDecimal.ZERO,
                BigDecimal.ZERO, false, List.of(), PostingEngine.readable(exception));
    }

    private Optional<JsonNode> sample(AccPostingRule rule) {
        if (StringUtils.hasText(rule.getSamplePayload())) {
            return Optional.of(template.read(rule.getSamplePayload()));
        }
        return eventRepository.search(null, rule.getEventType(), null, null, PageRequest.of(0, 1)).stream().findFirst()
                .map(event -> template.read(event.getPayload()));
    }

    // ------------------------------------------------------------ evenements

    @Transactional(readOnly = true)
    public List<PostingDtos.Event> events(PostingEventStatus status, String eventType, LocalDate from, LocalDate to, int limit) {
        return eventRepository.search(status, StringUtils.hasText(eventType) ? eventType.trim().toUpperCase(Locale.ROOT) : null, from, to,
                PageRequest.of(0, Math.max(1, Math.min(500, limit)))).stream().map(engine::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PostingDtos.EventSummary summary() {
        Map<PostingEventStatus, Long> counts = new EnumMap<>(PostingEventStatus.class);
        Arrays.stream(PostingEventStatus.values()).forEach(status -> counts.put(status, 0L));
        eventRepository.countByStatus().forEach(row -> counts.put((PostingEventStatus) row[0], (Long) row[1]));
        List<String> disabled = Arrays.stream(Optional.ofNullable(settings.disabledFamilies()).orElse("").split(","))
                .map(String::trim).filter(StringUtils::hasText).toList();
        return new PostingDtos.EventSummary(counts, settings.enabled(), settings.startDate(), disabled);
    }

    @Transactional(readOnly = true)
    public List<PostingDtos.Resolver> resolvers() {
        return template.resolvers().values().stream().map(resolver -> new PostingDtos.Resolver(resolver.name(), resolver.description())).toList();
    }

    private AccPostingRule rule(Long id) {
        return ruleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Schema d'ecriture introuvable : " + id));
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            if (!StringUtils.hasText(text)) {
                return null;
            }
            template.read(text);
            return text.trim();
        }
        return template.write(value);
    }

    PostingDtos.Rule toResponse(AccPostingRule rule) {
        return new PostingDtos.Rule(rule.getId(), rule.getCode(), rule.getVersion(), rule.getEventType(), rule.getLabel(), rule.getDescription(),
                rule.getFamily(), rule.getPriority(), rule.getConditions(), rule.getJournal(), rule.getEntryLabel(), rule.getLines(),
                rule.getNoEntry(), rule.getSamplePayload(), rule.getEffectiveFrom(), rule.getStatus(), rule.getActivatedBy(),
                rule.getActivatedAt(), rule.getCreatedBy(), rule.getCreatedAt());
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
