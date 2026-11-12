package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.JournalType;
import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import com.sni.bokaticowork.features.accounting.enums.PostingRuleStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccPostingEvent;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.repository.AccJournalRepository;
import com.sni.bokaticowork.features.accounting.repository.AccPendingEntryLogRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingChartService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingEngine;
import com.sni.bokaticowork.features.accounting.service.implementation.PostingRuleService;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.BasicPostingResolvers;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.accounting.service.support.PostingTemplate;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot F2 sur le schema de production, hors transaction de test : le moteur traite chaque
 * evenement dans sa propre transaction. Un journal propre au test evite de toucher a la
 * numerotation des autres tests.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, com.sni.bokaticowork.features.accounting.service.implementation.AnalyticAllocator.class, AccountingLetteringService.class,
        EntryNumbering.class, CashActorResolver.class, PostingEngine.class, PostingRuleService.class, PostingTemplate.class,
        BasicPostingResolvers.class, PostingEngineF2IT.Json.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostingEngineF2IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger PROVISIONAL = new AtomicInteger(1);

    @TestConfiguration
    static class Json {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @Autowired private PostingEngine engine;
    @Autowired private PostingRuleService rules;
    @Autowired private AccountingPeriodService periods;
    @Autowired private AccountingChartService chart;
    @Autowired private AccJournalRepository journals;
    @Autowired private AccEntryRepository entryRepository;
    @Autowired private AccPendingEntryLogRepository pendingRepository;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    private String journal;
    private String eventType;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("compta@elleaose.com", null,
                List.of(new SimpleGrantedAuthority("ACCOUNTING_ENTRY_BALANCE"))));
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-F2-" + PROVISIONAL.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
        when(settings.enabled()).thenReturn(true);
        when(settings.familyEnabled(anyString())).thenReturn(true);
        when(settings.startDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(settings.postingBatchSize()).thenReturn(50);
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.strictChronology()).thenReturn(true);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.unbalancedBlocksPeriodClose()).thenReturn(true);
        when(settings.letteringDifferenceThreshold()).thenReturn(new BigDecimal("500"));
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        journal = "F2" + suffix;
        eventType = "IT_SALE_" + suffix;
        periods.createJournal(new AccountingRequests.CreateJournal(journal, "Journal du test F2", JournalType.SALES, null, null, true, null, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Rejouer un evenement ne double pas l'ecriture")
    void idempotent() {
        activeRule("7061");
        AccPostingEvent first = engine.receive(eventType, "FAC-1", "SALES", LocalDate.of(2026, 2, 3), sale("FAC-1", "11800", "10000", "1800"), false);
        AccPostingEvent again = engine.receive(eventType, "FAC-1", "SALES", LocalDate.of(2026, 2, 3), sale("FAC-1", "99", "99", "0"), false);
        assertThat(again.getId()).isEqualTo(first.getId());

        AccPostingEvent posted = engine.process(first.getId());
        engine.process(first.getId());
        assertThat(posted.getStatus()).isEqualTo(PostingEventStatus.POSTED);
        assertThat(posted.getEntryNumber()).startsWith(journal + "-2026-");
        assertThat(entryRepository.findBySourceTypeAndSourceCode(eventType, "FAC-1")).hasSize(1)
                .first().satisfies(entry -> {
                    assertThat(entry.getStatus()).isEqualTo(EntryStatus.VALIDATED);
                    assertThat(entry.getTotalDebit()).isEqualByComparingTo("11800");
                    assertThat(entry.getPostingEventId()).isEqualTo(first.getId());
                });
    }

    @Test
    @DisplayName("Une variable non resolue met l'evenement en echec avec un motif lisible, sans exception")
    void missingVariableFails() {
        activeRule("7061");
        AccPostingEvent event = engine.receive(eventType, "FAC-2", "SALES", LocalDate.of(2026, 2, 4),
                "{\"documentNumber\":\"FAC-2\",\"totalTtc\":100,\"totalHt\":100,\"vat\":0}", false);
        AccPostingEvent result = engine.process(event.getId());
        assertThat(result.getStatus()).isEqualTo(PostingEventStatus.FAILED);
        assertThat(result.getFailureReason()).contains("customerCode");
        assertThat(entryRepository.findBySourceTypeAndSourceCode(eventType, "FAC-2")).isEmpty();
    }

    @Test
    @DisplayName("Sans schema actif, l'evenement est en echec ; activer le schema le remet en file")
    void noRuleThenRequeued() {
        AccPostingEvent event = engine.receive(eventType, "FAC-3", "SALES", LocalDate.of(2026, 2, 5), sale("FAC-3", "1180", "1000", "180"), false);
        assertThat(engine.process(event.getId()).getStatus()).isEqualTo(PostingEventStatus.FAILED);

        activeRule("7061");
        assertThat(engine.event(event.getId()).getStatus()).isEqualTo(PostingEventStatus.PENDING);
        assertThat(engine.process(event.getId()).getStatus()).isEqualTo(PostingEventStatus.POSTED);
    }

    @Test
    @DisplayName("Un schema desequilibre ne s'active pas")
    void unbalancedRuleIsNotActivated() {
        PostingDtos.Rule draft = rules.save(rule("TTC_SANS_TVA", "totalHt"));
        assertThatThrownBy(() -> rules.activate(draft.id())).isInstanceOf(BadRequestException.class).hasMessageContaining("desequilibree");
        assertThat(rules.versions(draft.code()).get(0).status()).isEqualTo(PostingRuleStatus.DRAFT);
    }

    @Test
    @DisplayName("Un compte absent du plan met la piece en attente ; une fois le compte cree, l'evenement se regenere et se valide")
    void unbalancedThenRegenerated() {
        // Le compte de produit vient de l'evenement : l'exemple du schema porte un compte existant.
        PostingDtos.Rule saved = rules.save(request("R_VAR_" + eventType, "{productAccount}", "totalTtc"));
        rules.activate(saved.id());
        String missing = "706" + (10 + Math.abs(eventType.hashCode() % 80));
        AccPostingEvent event = engine.receive(eventType, "FAC-4", "SALES", LocalDate.of(2026, 2, 6),
                "{\"documentNumber\":\"FAC-4\",\"customerCode\":\"CUS-IT-2\",\"totalTtc\":1180,\"totalHt\":1000,\"vat\":180,"
                        + "\"productAccount\":\"" + missing + "9\"}", false);
        AccPostingEvent pending = engine.process(event.getId());
        assertThat(pending.getStatus()).isEqualTo(PostingEventStatus.UNBALANCED);
        assertThat(pending.getEntryNumber()).isNull();
        assertThat(pending.getFailureReason()).contains(missing + "9");
        AccEntry held = entryRepository.findByProvisionalNumber(pending.getProvisionalNumber()).orElseThrow();
        assertThat(held.getStatus()).isEqualTo(EntryStatus.PENDING_BALANCE);

        chart.create(new AccountingRequests.CreateAccount("706", missing + "9", "Produit cree pour le test F2", null, null, null, null, null,
                null, "Test"));
        AccPostingEvent regenerated = engine.regenerate(event.getId());
        assertThat(regenerated.getStatus()).isEqualTo(PostingEventStatus.POSTED);
        assertThat(regenerated.getEntryNumber()).startsWith(journal + "-2026-");
        assertThat(entryRepository.findById(held.getId()).orElseThrow().getStatus()).isEqualTo(EntryStatus.REJECTED);
        assertThat(pendingRepository.findByEntryIdOrderByCreatedAtDesc(held.getId())).first()
                .satisfies(log -> assertThat(log.getResolution()).isEqualTo("REGENERATED"));
        assertThatThrownBy(() -> engine.regenerate(event.getId())).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Une nouvelle version activee remplace l'ancienne pour les evenements suivants")
    void newVersionReplacesTheOldOne() {
        PostingDtos.Rule first = activeRule("7061");
        rules.save(request(first.code(), "7062", "totalTtc"));
        PostingDtos.Rule second = rules.activate(rules.versions(first.code()).get(0).id());
        assertThat(second.version()).isEqualTo(2);
        assertThat(rules.versions(first.code())).extracting(PostingDtos.Rule::status)
                .containsExactly(PostingRuleStatus.ACTIVE, PostingRuleStatus.RETIRED);
        AccPostingEvent event = engine.receive(eventType, "FAC-8", "SALES", LocalDate.of(2026, 2, 9), sale("FAC-8", "1180", "1000", "180"), false);
        assertThat(engine.process(event.getId()).getRuleVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Un schema sans ecriture ecarte l'evenement")
    void noEntryRule() {
        PostingDtos.Rule rule = rules.save(new PostingDtos.RuleRequest("NONE_" + eventType, eventType, "Pas d'ecriture", null, "SALES", 10,
                Map.of("kind", "FREE"), null, null, null, true, null, LocalDate.of(2026, 1, 1)));
        rules.activate(rule.id());
        AccPostingEvent event = engine.receive(eventType, "FAC-5", "SALES", LocalDate.of(2026, 2, 7), "{\"kind\":\"FREE\"}", false);
        AccPostingEvent result = engine.process(event.getId());
        assertThat(result.getStatus()).isEqualTo(PostingEventStatus.IGNORED);
        assertThat(result.getRuleCode()).isEqualTo(rule.code());
    }

    @Test
    @DisplayName("La reprise produit des brouillards, valides ensuite en bloc")
    void replayProducesDrafts() {
        activeRule("7061");
        AccPostingEvent event = engine.receive(eventType, "FAC-6", "SALES", LocalDate.of(2026, 1, 15), sale("FAC-6", "590", "500", "90"), true);
        AccPostingEvent result = engine.process(event.getId());
        assertThat(result.getStatus()).isEqualTo(PostingEventStatus.POSTED);
        assertThat(result.getEntryNumber()).isNull();

        PostingDtos.BatchResult batch = engine.validateDrafts(journal, null);
        assertThat(batch.succeeded()).isEqualTo(1);
        assertThat(engine.event(event.getId()).getEntryNumber()).isEqualTo(journal + "-2026-000001");
    }

    @Test
    @DisplayName("Apercu d'un schema en cours d'edition, sans rien enregistrer")
    void preview() {
        PostingDtos.Preview preview = rules.preview(new PostingDtos.PreviewRequest(null, rule("APERCU", "totalTtc"), null,
                Map.of("documentNumber", "FAC-7", "customerCode", "CUS-9", "totalTtc", 1180, "totalHt", 1000, "vat", 180)));
        assertThat(preview.balanced()).isTrue();
        assertThat(preview.journalCode()).isEqualTo(journal);
        assertThat(preview.lines()).hasSize(3).allSatisfy(line -> assertThat(line.accountExists()).isTrue());
        assertThat(preview.label()).isEqualTo("Facture FAC-7");
    }

    private PostingDtos.Rule activeRule(String productAccount) {
        PostingDtos.Rule saved = rules.save(request("R_" + eventType + "_" + productAccount, productAccount, "totalTtc"));
        return rules.activate(saved.id());
    }

    private PostingDtos.RuleRequest rule(String code, String clientAmount) {
        return request(code + "_" + eventType, "7061", clientAmount);
    }

    private PostingDtos.RuleRequest request(String code, String productAccount, String clientAmount) {
        return new PostingDtos.RuleRequest(code, eventType, "Facture emise", null, "SALES", 100, null, journal, "Facture {documentNumber}",
                List.of(Map.of("side", "DEBIT", "account", "411", "amount", clientAmount, "thirdParty", "{customerCode}", "reference", "{documentNumber}"),
                        Map.of("side", "CREDIT", "account", productAccount, "amount", "totalHt"),
                        Map.of("side", "CREDIT", "account", "4431", "amount", "vat")),
                false, Map.of("documentNumber", "EX-1", "customerCode", "CUS-1", "totalTtc", 1180, "totalHt", 1000, "vat", 180, "productAccount", "7061"),
                LocalDate.of(2026, 1, 1));
    }

    private static String sale(String number, String ttc, String ht, String vat) {
        return "{\"documentNumber\":\"" + number + "\",\"customerCode\":\"CUS-IT-2\",\"totalTtc\":" + ttc + ",\"totalHt\":" + ht + ",\"vat\":" + vat + "}";
    }
}
