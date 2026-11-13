package com.sni.bokaticowork.features.fund.voucher;

import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.fund.dto.FundDtos;
import com.sni.bokaticowork.features.fund.enums.FundApprovalDecision;
import com.sni.bokaticowork.features.fund.enums.FundNeedStatus;
import com.sni.bokaticowork.features.fund.enums.FundRequestStatus;
import com.sni.bokaticowork.features.fund.enums.FundRequestType;
import com.sni.bokaticowork.features.fund.enums.FundUrgency;
import com.sni.bokaticowork.features.fund.service.FundNeedService;
import com.sni.bokaticowork.features.fund.service.FundRequestService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot D2 : les pieces de fonds d'exemple (apercu des modeles) portent les memes cles que les pieces
 * reelles, et l'apercu d'une piece reelle n'attribue aucun numero.
 */
class FundVoucherSamplesTest {

    private final FundNeedService needs = mock(FundNeedService.class);
    private final FundRequestService requests = mock(FundRequestService.class);
    private final VoucherService vouchers = Mockito.spy(new VoucherService(null, null, new QrCodeRenderer()));
    private final CashActorResolver actors = mock(CashActorResolver.class);
    private final FundVoucherService service = new FundVoucherService(needs, requests, vouchers, actors);

    @Test
    void needSampleHasTheKeysOfARealNeed() {
        Instant at = Instant.parse("2026-11-08T08:15:00Z");
        when(needs.get("EDB-1")).thenReturn(new FundDtos.Need("EDB-1", "Grâce", "Accueil", "PNR", "ACCUEIL", "Fournitures", FundUrgency.NORMAL,
                LocalDate.of(2026, 11, 14), FundNeedStatus.TRANSFORMED, new BigDecimal("100000"), null, at, "P. Moussa", at, "Trois devis", null, at,
                List.of(new FundDtos.NeedLine(1, "Papier", BigDecimal.TEN, "ramette", new BigDecimal("10000"), new BigDecimal("100000"), "FOURN",
                        "Papeterie", "DDF-9"))));
        stubPeek();

        DocumentData real = service.previewData("NEED_STATEMENT", "EDB-1");

        assertSameKeys(FundVoucherSamples.sample("NEED_STATEMENT"), real);
        verify(vouchers, never()).issueAndPrint(anyString(), anyString(), anyString(), any(), any(), any());
        assertThat(real.number()).isEqualTo("APERÇU");
    }

    @Test
    void requestSampleHasTheKeysOfARealRequest() {
        Instant at = Instant.parse("2026-11-10T08:40:00Z");
        FundDtos.Request request = mock(FundDtos.Request.class);
        when(request.number()).thenReturn("DDF-1");
        when(request.type()).thenReturn(FundRequestType.DIRECT_EXPENSE);
        when(request.status()).thenReturn(FundRequestStatus.APPROVED);
        when(request.requester()).thenReturn("Grâce");
        when(request.requesterDepartment()).thenReturn("Accueil");
        when(request.beneficiaryName()).thenReturn("Papeterie");
        when(request.registerCode()).thenReturn("CAISSE-1");
        when(request.deliveryMode()).thenReturn(com.sni.bokaticowork.features.fund.enums.FundDeliveryMode.CASH);
        when(request.subject()).thenReturn("Fournitures");
        when(request.totalAmount()).thenReturn(new BigDecimal("130500"));
        when(request.wantedDate()).thenReturn(LocalDate.of(2026, 11, 14));
        when(request.needStatementNumber()).thenReturn("EDB-1");
        when(request.createdAt()).thenReturn(at);
        when(request.submittedAt()).thenReturn(at);
        when(request.lines()).thenReturn(List.of(new FundDtos.RequestLine("FOURN", "Papier", new BigDecimal("130500"), "Papeterie", "ACCUEIL")));
        when(request.timeline()).thenReturn(List.of(
                new FundDtos.Approval(0, "Grâce", FundApprovalDecision.values()[0], new BigDecimal("130500"), null, null, null, at),
                new FundDtos.Approval(1, "P. Moussa", FundApprovalDecision.values()[0], new BigDecimal("130500"), null, null, null, at),
                new FundDtos.Approval(2, "R. Ibara", FundApprovalDecision.values()[0], new BigDecimal("130500"), null, null, null, at)));
        FundDtos.Insight insight = mock(FundDtos.Insight.class);
        when(insight.levelLabels()).thenReturn(List.of("Chef de service", "Comptable"));
        when(insight.openAdvances()).thenReturn(List.of());
        when(request.insight()).thenReturn(insight);
        when(requests.get("DDF-1")).thenReturn(request);
        stubPeek();

        DocumentData real = service.previewData("FUND_REQUEST", "DDF-1");

        DocumentData sample = FundVoucherSamples.sample("FUND_REQUEST");
        assertSameKeys(sample, real);
        assertThat(real.signers().keySet()).contains("demandeur", "niveau_1", "niveau_2");
    }

    private void stubPeek() {
        doAnswer(invocation -> new VoucherService.Issued("APERÇU", invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
                invocation.getArgument(3), "tester", Instant.now(), 0, null, null))
                .when(vouchers).peek(anyString(), anyString(), any(), any(), any());
    }

    /** Chaque cle de l'exemple existe sur la piece reelle (champs, tableaux et leurs colonnes, signataires). */
    private static void assertSameKeys(DocumentData sample, DocumentData real) {
        assertThat(real.fields().keySet()).containsAll(sample.fields().keySet().stream().filter(key -> !key.equals("exploitant")).toList());
        assertThat(real.tables().keySet()).containsAll(sample.tables().keySet());
        sample.tables().forEach((key, table) -> assertThat(columns(real.tables().get(key))).isEqualTo(columns(table)));
        assertThat(real.signers().keySet()).containsAll(sample.signers().keySet());
    }

    private static Set<String> columns(DocumentData.Table table) {
        return table.columns().stream().map(DocumentData.Column::key).collect(Collectors.toSet());
    }
}
