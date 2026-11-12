package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.core.accounting.AccountingEventPublisher;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.accounting.enums.UnbalancedPolicy;
import com.sni.bokaticowork.features.accounting.model.FixedAssetDisposal;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
import com.sni.bokaticowork.features.accounting.service.support.AccountingNotifier;
import com.sni.bokaticowork.features.accounting.service.support.AccountingSettings;
import com.sni.bokaticowork.features.accounting.service.support.EntryNumbering;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
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
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Lot F18 sur le schema de production, critere d'acceptation du plan : un ordinateur de 900 000 HT
 * mis en service le 1er avril amortit 25 000 par mois ; relancer ne double rien ; la cession produit
 * une moins-value de 25 000 ; le fichier egale les comptes ; une campagne signale un bien non vu.
 */
@Import({AccountingChartService.class, AccountingPeriodService.class, AccountingEntryService.class, AnalyticAllocator.class,
        AccountingLetteringService.class, EntryNumbering.class, CashActorResolver.class, FixedAssetService.class, DepreciationRunService.class,
        FixedAssetReportService.class, FixedAssetInventoryService.class, VoucherService.class, QrCodeRenderer.class, FixedAssetF18IT.Templates.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FixedAssetF18IT extends PostgresIntegrationTestBase {

    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private static final String[] RIGHTS = {"FIXED_ASSET_READ", "FIXED_ASSET_MANAGE", "FIXED_ASSET_REVISE", "FIXED_ASSET_IMPAIR",
            "FIXED_ASSET_DISPOSE", "FIXED_ASSET_DEPRECIATION_RUN", "FIXED_ASSET_INVENTORY", "ACCOUNTING_ENTRY_BALANCE"};

    @TestConfiguration
    static class Templates {
        @Bean
        SpringTemplateEngine templateEngine() {
            org.thymeleaf.templateresolver.ClassLoaderTemplateResolver resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            return engine;
        }
    }

    @Autowired private FixedAssetService assets;
    @Autowired private DepreciationRunService runs;
    @Autowired private FixedAssetReportService reports;
    @Autowired private FixedAssetInventoryService inventories;
    @MockitoBean private AccountingSettings settings;
    @MockitoBean private AccountingNotifier accountingNotifier;
    @MockitoBean private SequenceGeneratorFacade sequences;
    @MockitoBean private CashRegisterService cashRegisterService;
    @MockitoBean private AccountingEventPublisher accountingEvents;

    @BeforeEach
    void setUp() {
        when(sequences.next(anyString())).thenAnswer(invocation -> invocation.getArgument(0, String.class).toUpperCase() + "-F18-" + SEQ.getAndIncrement());
        when(settings.enabled()).thenReturn(true);
        when(settings.unbalancedPolicy()).thenReturn(UnbalancedPolicy.HOLD);
        when(settings.numberingWidth()).thenReturn(6);
        when(settings.strictChronology()).thenReturn(false);
        when(settings.text(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
        as("comptable@elleaose.com", RIGHTS);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Acquisition, mise en service au 1er avril, dotations de 25 000 sans doublon, cession avec moins-value de 25 000")
    void acceptance() {
        FixedAssetService.AssetView created = assets.create(request("Ordinateur portable F18", new BigDecimal("900000"), LocalDate.of(2026, 3, 20)));
        String code = created.asset().getCode();
        assertThat(created.asset().getAccountingStatus()).isEqualTo("DRAFT");
        assertThat(created.asset().getBooked()).isTrue();

        assets.commission(code, new FixedAssetService.CommissionRequest(LocalDate.of(2026, 4, 1), "PNR", null, "Accueil", null, null,
                null, null, "Recu en bon etat"));
        FixedAssetService.PlanView plan = assets.plan(code, null);
        assertThat(plan).isNotNull();

        DepreciationRunService.RunResult april = runs.run("2026-04");
        BigDecimal mine = april.lines().stream().filter(line -> code.equals(line.assetCode())).map(DepreciationRunService.RunLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(mine).isEqualByComparingTo("25000");

        DepreciationRunService.RunResult again = runs.run("2026-04");
        assertThat(again.lines()).noneMatch(line -> code.equals(line.assetCode()));

        runs.run("2026-05");
        assertThat(assets.view(code).asset().getAccumulatedDepreciation()).isEqualByComparingTo("50000");

        FixedAssetDisposal disposal = assets.prepareDisposal(code, new FixedAssetService.DisposalRequest("SALE", LocalDate.of(2026, 5, 31),
                new BigDecimal("825000"), BigDecimal.ZERO, "Acheteur F18", "Bon de cession", false));
        assertThatThrownBy(() -> assets.validateDisposal(disposal.getId())).isInstanceOfAny(ForbiddenException.class, BadRequestException.class);

        as("daf@elleaose.com", RIGHTS);
        FixedAssetDisposal validated = assets.validateDisposal(disposal.getId());
        assertThat(validated.getGainLoss()).isEqualByComparingTo("-25000");
        assertThat(validated.getNetValue()).isEqualByComparingTo("850000");
        assertThat(assets.view(code).asset().getAccountingStatus()).isEqualTo("DISPOSED");

        FixedAssetReportService.Table note = reports.report("note-3d", 2026, null, null, null, null, null);
        assertThat(note.rows()).anyMatch(row -> code.equals(row.get(0)) && new BigDecimal("-25000").compareTo((BigDecimal) row.get(9)) == 0);
        assertThat(reports.csv(note)).isNotEmpty();
        assertThat(reports.pdf(note)).isNotEmpty();
    }

    @Test
    @DisplayName("Le fichier egale les comptes 2x et 28x ; une campagne signale le bien non vu et produit son PV")
    void reconciliationAndInventory() {
        FixedAssetService.AssetView created = assets.create(request("Ecran de salle F18", new BigDecimal("600000"), LocalDate.of(2026, 2, 10)));
        String code = created.asset().getCode();
        assets.commission(code, new FixedAssetService.CommissionRequest(LocalDate.of(2026, 3, 1), "F18SITE", null, "Salle", null, null,
                null, null, null));
        runs.run("2026-03");

        FixedAssetReportService.Table reconciliation = reports.report("reconciliation", null, null, null, null, null, null);
        assertThat(reconciliation.rows()).filteredOn(row -> List.of("2442", "28442").contains(row.get(0)))
                .allMatch(row -> "OK".equals(row.get(4)));

        FixedAssetInventoryService.CampaignView campaign = inventories.open(new FixedAssetInventoryService.CampaignRequest("Inventaire F18",
                "F18SITE", null, null, List.of("comptable@elleaose.com"), LocalDate.of(2026, 11, 1), "Temoin"));
        assertThat(campaign.expected()).contains(code);

        String campaignCode = campaign.campaign().getCode();
        assertThatThrownBy(() -> inventories.close(campaignCode, new FixedAssetInventoryService.CloseRequest(null, null)))
                .isInstanceOf(BadRequestException.class);
        FixedAssetInventoryService.CampaignView closed = inventories.close(campaignCode,
                new FixedAssetInventoryService.CloseRequest("Bien a rechercher dans les reserves", null));
        assertThat(closed.missing()).contains(code);
        assertThat(closed.campaign().getVoucherNumber()).isNotBlank();
        assertThat(inventories.reportPdf(campaignCode)).isNotEmpty();
        assertThat(inventories.labels(List.of(code))).isNotEmpty();
    }

    private static FixedAssetService.AssetRequest request(String designation, BigDecimal value, LocalDate acquisition) {
        return new FixedAssetService.AssetRequest("MATERIEL_INFORMATIQUE", designation, "Marque", "Modele", null, 1, "PURCHASE", "FRN-F18",
                "Fournisseur F18", null, null, null, null, acquisition, acquisition, value, BigDecimal.ZERO, BigDecimal.ZERO, false, "481", null,
                null, null, null, null, null, Map.of(), null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, List.of());
    }

    private static void as(String user, String... rights) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null,
                Arrays.stream(rights).map(SimpleGrantedAuthority::new).toList()));
    }
}
