package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.dto.AccountingResponses;
import com.sni.bokaticowork.features.accounting.dto.PostingDtos;
import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.JournalType;
import com.sni.bokaticowork.features.accounting.enums.PostingEventStatus;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccPostingEvent;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingChartService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingEntryService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingLetteringService;
import com.sni.bokaticowork.features.accounting.service.implementation.AccountingPeriodService;
import com.sni.bokaticowork.features.accounting.service.implementation.EntryCorrectionService;
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
 * Lot K1 de la revue comptable sur le schema de production : contre-passation hors du moteur,
 * recomptabilisation, reclassement, correction, seconde validation, chronologie limitee au mois,
 * rejet d'une piece d'evenement. Journaux propres au test (numerotation independante).
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class,
        com.sni.bokaticowork.features.accounting.service.implementation.AnalyticAllocator.class, AccountingLetteringService.class,
        EntryNumbering.class, CashActorResolver.class, PostingEngine.class, PostingRuleService.class, PostingTemplate.class,
        BasicPostingResolvers.class, EntryCorrectionService.class, K1CorrectionsIT.Json.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class K1CorrectionsIT extends PostgresIntegrationTestBase {

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
    @Autowired private AccountingEntryService entries;
    @Autowired private EntryCorrectionService corrections;
    @Autowired private AccountingLetteringService lettering;
    @Autowired private AccEntryRepository entryRepository;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier notifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private PlatformSettings platformSettings;

    private String journal;
    private String correctionJournal;
    private String eventType;

    @BeforeEach
    void setUp() {
        com.sni.bokaticowork.core.settings.platform.PlatformSettingsStubs.fallbacks(platformSettings);
        login("compta@elleaose.com");
        when(sequences.next(anyString())).thenAnswer(invocation -> "BR-K1-" + PROVISIONAL.getAndIncrement() + "-" + UUID.randomUUID().toString().substring(0, 6));
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
        journal = "K1" + suffix;
        correctionJournal = "KO" + suffix;
        eventType = "IT_K1_" + suffix;
        periods.createJournal(new AccountingRequests.CreateJournal(journal, "Journal du test K1", JournalType.SALES, null, null, true, null, null));
        periods.createJournal(new AccountingRequests.CreateJournal(correctionJournal, "Corrections du test K1", JournalType.MISC, null, null, true, null, null));
        when(settings.correctionJournal()).thenReturn(correctionJournal);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("S1 : contre-passer a la main une piece automatique passe l'evenement en REVERSED ; il se recomptabilise, pieces liees")
    void manualReversalThenRepost() {
        activeRule("7061");
        AccPostingEvent event = engine.process(engine.receive(eventType, "FAC-1", "SALES", LocalDate.of(2026, 2, 3),
                sale("FAC-1", "11800", "10000", "1800"), false).getId());
        assertThat(event.getStatus()).isEqualTo(PostingEventStatus.POSTED);
        String first = event.getEntryNumber();

        entries.reverse(first, new AccountingRequests.Reverse("Mauvais client", LocalDate.of(2026, 2, 4)));
        AccPostingEvent reversed = engine.event(event.getId());
        assertThat(reversed.getStatus()).isEqualTo(PostingEventStatus.REVERSED);
        assertThat(reversed.getFailureReason()).contains("contre-passée").contains("recomptabilisez");

        PostingDtos.RepostResult result = engine.repost(event.getId(), "Client corrige", null);
        assertThat(result.event().status()).isEqualTo(PostingEventStatus.POSTED);
        assertThat(result.newEntry()).isNotEqualTo(first);
        AccEntry fresh = entryRepository.findByEntryNumber(result.newEntry()).orElseThrow();
        assertThat(fresh.getCorrectionOf()).isEqualTo(first);
        assertThat(fresh.getCorrectionKind()).isEqualTo("REPOST");
        assertThat(entryRepository.findByEntryNumber(first).orElseThrow().getCorrectedBy()).isEqualTo(result.newEntry());
    }

    @Test
    @DisplayName("M6 : recomptabiliser un evenement comptabilise apres correction du schema ; apercu en lot avant")
    void repostAfterRuleChange() {
        PostingDtos.Rule v1 = activeRule("7061");
        AccPostingEvent event = engine.process(engine.receive(eventType, "FAC-2", "SALES", LocalDate.of(2026, 2, 5),
                sale("FAC-2", "11800", "10000", "1800"), false).getId());
        String first = event.getEntryNumber();
        // Le schema est corrige : le produit va en 7062.
        PostingDtos.Rule v2 = rules.save(request(v1.code(), "7062", "totalTtc"));
        rules.activate(v2.id());

        PostingDtos.RepostBatch preview = engine.repostBatch(new PostingDtos.RepostBatchRequest(v1.code(), null, null, null, true, null));
        assertThat(preview.result()).isNull();
        assertThat(preview.events()).singleElement().satisfies(item -> {
            assertThat(item.changed()).isTrue();
            assertThat(item.after()).extracting(PostingDtos.PreviewLine::accountNumber).contains("7062");
        });

        PostingDtos.RepostBatch done = engine.repostBatch(new PostingDtos.RepostBatchRequest(v1.code(), null, null, null, false, "Produit en 7062"));
        assertThat(done.result().succeeded()).isEqualTo(1);
        AccPostingEvent reposted = engine.event(event.getId());
        assertThat(reposted.getStatus()).isEqualTo(PostingEventStatus.POSTED);
        assertThat(entries.get(reposted.getEntryNumber()).lines()).extracting(AccountingResponses.EntryLine::accountNumber).contains("7062");
        assertThat(entryRepository.findByEntryNumber(first).orElseThrow().getStatus()).isEqualTo(EntryStatus.REVERSED);
    }

    @Test
    @DisplayName("S2 : reclasser une ligne (compte, puis tiers) dans une piece OD liee ; la ligne client reclassee est lettree avec sa sortie")
    void reclassify() {
        AccountingResponses.Entry sale = entries.create(manual(LocalDate.of(2026, 5, 6), "11800", "CUS-A", "7061"));
        AccountingResponses.Entry reclass = entries.reclassify(sale.entryNumber(), new AccountingRequests.Reclassify("Mauvais produit", null,
                List.of(new AccountingRequests.ReclassLine(2, "7062", null, null, null))));
        assertThat(reclass.status()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(reclass.journalCode()).isEqualTo(correctionJournal);
        assertThat(reclass.entryDate()).isEqualTo(LocalDate.of(2026, 5, 6));
        assertThat(reclass.correctionOf()).isEqualTo(sale.entryNumber());
        assertThat(reclass.correctionKind()).isEqualTo("RECLASS");
        assertThat(reclass.lines()).extracting(AccountingResponses.EntryLine::accountNumber).containsExactly("7061", "7062");
        assertThat(reclass.lines().get(0).debit()).isEqualByComparingTo("11800");
        assertThat(entries.get(sale.entryNumber()).correctedBy()).isEqualTo(reclass.entryNumber());

        AccountingResponses.Entry customer = entries.reclassify(sale.entryNumber(), new AccountingRequests.Reclassify("Mauvais client", null,
                List.of(new AccountingRequests.ReclassLine(1, null, "CUS-B", null, null))));
        assertThat(customer.lines()).extracting(AccountingResponses.EntryLine::thirdPartyCode).containsExactly("CUS-A", "CUS-B");
        Long originalLine = entries.get(sale.entryNumber()).lines().get(0).id();
        assertThat(lettering.activeCode(originalLine)).isPresent();

        assertThatThrownBy(() -> entries.reclassify(sale.entryNumber(), new AccountingRequests.Reclassify("Encore", null,
                List.of(new AccountingRequests.ReclassLine(1, null, "CUS-C", null, null)))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("délettrez");
    }

    @Test
    @DisplayName("S2 : corriger une piece manuelle, contre-passation et piece juste liees ; une piece automatique refuse des lignes")
    void correctManualEntry() {
        AccountingResponses.Entry wrong = entries.create(manual(LocalDate.of(2026, 6, 2), "5000", "CUS-A", "7061"));
        AccountingResponses.Correction correction = corrections.correct(wrong.entryNumber(), new AccountingRequests.Correct("Montant faux", null, null,
                "Facture corrigee", List.of(line("411", "CUS-A", "5900", null), line("7061", null, null, "5900"))));
        assertThat(correction.reversal().status()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(correction.correction().status()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(correction.correction().correctionOf()).isEqualTo(wrong.entryNumber());
        AccountingResponses.Entry original = entries.get(wrong.entryNumber());
        assertThat(original.status()).isEqualTo(EntryStatus.REVERSED);
        assertThat(original.correctedBy()).isEqualTo(correction.correction().entryNumber());

        activeRule("7061");
        AccPostingEvent event = engine.process(engine.receive(eventType, "FAC-3", "SALES", LocalDate.of(2026, 6, 3),
                sale("FAC-3", "1180", "1000", "180"), false).getId());
        assertThatThrownBy(() -> corrections.correct(event.getEntryNumber(), new AccountingRequests.Correct("x", null, null, null,
                List.of(line("411", "CUS-A", "1", null), line("7061", null, null, "1")))))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("automatique");
    }

    @Test
    @DisplayName("S11 : au-dela du seuil, la contre-passation attend un autre comptable")
    void secondValidationOnReversal() {
        AccountingResponses.Entry entry = entries.create(manual(LocalDate.of(2026, 7, 1), "2000000", "CUS-A", "7061"));
        when(settings.secondValidationThreshold()).thenReturn(new BigDecimal("1000000"));
        AccountingResponses.Entry draft = entries.reverse(entry.entryNumber(), new AccountingRequests.Reverse("Doublon", LocalDate.of(2026, 7, 2)));
        assertThat(draft.status()).isEqualTo(EntryStatus.DRAFT);
        assertThat(draft.awaitingSecondValidation()).isTrue();
        assertThat(entries.get(entry.entryNumber()).status()).isEqualTo(EntryStatus.VALIDATED);
        assertThatThrownBy(() -> entries.validate(draft.provisionalNumber())).isInstanceOf(ForbiddenException.class);

        login("controle@elleaose.com");
        AccountingResponses.Entry validated = entries.validate(draft.provisionalNumber());
        assertThat(validated.status()).isEqualTo(EntryStatus.VALIDATED);
        assertThat(entries.get(entry.entryNumber()).status()).isEqualTo(EntryStatus.REVERSED);
    }

    @Test
    @DisplayName("D3 / M5 : un fait en retard garde sa date si son mois est ouvert ; dans le mois, il prend la derniere date du journal")
    void lateFactKeepsItsMonth() {
        activeRule("7061");
        engine.process(engine.receive(eventType, "FAC-AVR", "SALES", LocalDate.of(2026, 4, 12), sale("FAC-AVR", "1180", "1000", "180"), false).getId());
        AccPostingEvent march = engine.process(engine.receive(eventType, "FAC-MAR", "SALES", LocalDate.of(2026, 3, 31),
                sale("FAC-MAR", "1180", "1000", "180"), false).getId());
        assertThat(march.getStatus()).isEqualTo(PostingEventStatus.POSTED);
        assertThat(entries.get(march.getEntryNumber()).entryDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(march.getShiftedNote()).isNull();

        AccPostingEvent earlier = engine.process(engine.receive(eventType, "FAC-MAR2", "SALES", LocalDate.of(2026, 3, 20),
                sale("FAC-MAR2", "1180", "1000", "180"), false).getId());
        assertThat(entries.get(earlier.getEntryNumber()).entryDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(earlier.getShiftedNote()).contains("2026-03-31");

        // Saisie manuelle : une piece de mars apres une piece d'avril est acceptee ; dans mars, l'ordre reste impose.
        AccountingResponses.Entry manualMarch = entries.create(manualIn(journal, LocalDate.of(2026, 3, 31), "100", "CUS-A", "7061"));
        assertThat(manualMarch.status()).isEqualTo(EntryStatus.VALIDATED);
        assertThatThrownBy(() -> entries.create(manualIn(journal, LocalDate.of(2026, 3, 2), "100", "CUS-A", "7061")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("même mois");
    }

    @Test
    @DisplayName("S3 / S4 : une piece d'evenement modifiee garde la trace du schema ; rejetee, l'evenement repasse en echec")
    void rejectAndTraceAutomaticEntry() {
        // Fait sans client : la ligne 411 n'a pas de tiers, la piece part en attente.
        PostingDtos.Rule saved = rules.save(new PostingDtos.RuleRequest("R_" + eventType + "_OPT", eventType, "Facture emise", null, "SALES", 100, null,
                journal, "Facture {documentNumber}",
                List.of(Map.of("side", "DEBIT", "account", "411", "amount", "totalTtc", "thirdParty", "{?customerCode}", "reference", "{documentNumber}"),
                        Map.of("side", "CREDIT", "account", "7061", "amount", "totalHt"),
                        Map.of("side", "CREDIT", "account", "4431", "amount", "vat")),
                false, Map.of("documentNumber", "EX-1", "customerCode", "CUS-1", "totalTtc", 1180, "totalHt", 1000, "vat", 180),
                LocalDate.of(2026, 1, 1)));
        rules.activate(saved.id());
        AccPostingEvent event = engine.process(engine.receive(eventType, "FAC-4", "SALES", LocalDate.of(2026, 8, 3),
                "{\"documentNumber\":\"FAC-4\",\"totalTtc\":1180,\"totalHt\":1000,\"vat\":180}", false).getId());
        assertThat(event.getStatus()).isEqualTo(PostingEventStatus.UNBALANCED);

        assertThatThrownBy(() -> entries.update(event.getProvisionalNumber(), new AccountingRequests.Entry(correctionJournal, LocalDate.of(2026, 8, 3),
                null, "Autre journal", null, null, List.of(line("411", "CUS-A", "1180", null), line("7061", null, null, "1180")), false, "essai")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("schéma");
        AccountingResponses.Entry changed = entries.update(event.getProvisionalNumber(), new AccountingRequests.Entry(journal, LocalDate.of(2026, 8, 3),
                null, "Facture FAC-4", null, null, List.of(line("411", null, "1180", null), line("7061", null, null, "1000"),
                line("4431", null, null, "180")), false, "Montant client corrige"));
        assertThat(changed.generatedLines()).hasSize(3);
        assertThat(changed.manuallyChangedBy()).isEqualTo("compta@elleaose.com");
        assertThat(changed.changeReason()).isEqualTo("Montant client corrige");

        entries.reject(event.getProvisionalNumber(), "Schema faux");
        AccPostingEvent failed = engine.event(event.getId());
        assertThat(failed.getStatus()).isEqualTo(PostingEventStatus.FAILED);
        assertThat(failed.getFailureReason()).contains("rejetée").contains("Schema faux");
    }

    // ------------------------------------------------------------------ outils

    private void login(String user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ACCOUNTING_ENTRY_BALANCE"))));
    }

    private PostingDtos.Rule activeRule(String productAccount) {
        PostingDtos.Rule saved = rules.save(request("R_" + eventType + "_" + productAccount, productAccount, "totalTtc"));
        return rules.activate(saved.id());
    }

    private PostingDtos.RuleRequest request(String code, String productAccount, String clientAmount) {
        return new PostingDtos.RuleRequest(code, eventType, "Facture emise", null, "SALES", 100, null, journal, "Facture {documentNumber}",
                List.of(Map.of("side", "DEBIT", "account", "411", "amount", clientAmount, "thirdParty", "{customerCode}", "reference", "{documentNumber}"),
                        Map.of("side", "CREDIT", "account", productAccount, "amount", "totalHt"),
                        Map.of("side", "CREDIT", "account", "4431", "amount", "vat")),
                false, Map.of("documentNumber", "EX-1", "customerCode", "CUS-1", "totalTtc", 1180, "totalHt", 1000, "vat", 180, "productAccount", "7061"),
                LocalDate.of(2026, 1, 1));
    }

    private AccountingRequests.Entry manual(LocalDate date, String amount, String customer, String product) {
        return manualIn(journal, date, amount, customer, product);
    }

    private static AccountingRequests.Entry manualIn(String journalCode, LocalDate date, String amount, String customer, String product) {
        return new AccountingRequests.Entry(journalCode, date, null, "Facture manuelle", null, null,
                List.of(line("411", customer, amount, null), line(product, null, null, amount)), true);
    }

    private static AccountingRequests.EntryLine line(String account, String thirdParty, String debit, String credit) {
        return new AccountingRequests.EntryLine(account, thirdParty, null, debit == null ? null : new BigDecimal(debit),
                credit == null ? null : new BigDecimal(credit), null, null, null, null, null, null, null);
    }

    private static String sale(String number, String ttc, String ht, String vat) {
        return "{\"documentNumber\":\"" + number + "\",\"customerCode\":\"CUS-IT-2\",\"totalTtc\":" + ttc + ",\"totalHt\":" + ht + ",\"vat\":" + vat + "}";
    }
}
