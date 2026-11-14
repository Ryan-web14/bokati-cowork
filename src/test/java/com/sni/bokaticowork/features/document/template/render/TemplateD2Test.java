package com.sni.bokaticowork.features.document.template.render;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.features.document.template.service.DocumentTemplatePrinter;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.service.support.CashVoucherDocumentData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot D2 : controle du contenu d'un modele, jetons, cles derivees des libelles, et priorite d'une
 * version publiee sur le gabarit d'origine.
 */
class TemplateD2Test {

    @Test
    void contentValidationNamesEachError() {
        TemplateContent content = new TemplateContent("A3", List.of(), Map.of("accent", "rouge"), List.of(
                Map.of("type", "IFRAME"),
                Map.of("type", "FIELDS", "columns", 9, "fields", List.of("Date Souhaitée")),
                Map.of("type", "TEXT", "text", "ok", "onclick", "x"),
                Map.of("type", "SIGNATURES", "width", 500, "boxes", List.of(Map.of("signer", "caissier")))));

        assertThatThrownBy(content::validate).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Format inconnu : A3")
                .hasMessageContaining("Couleur d'accent invalide")
                .hasMessageContaining("bloc 1 : type inconnu « IFRAME »")
                .hasMessageContaining("de une à quatre colonnes")
                .hasMessageContaining("clé invalide « Date Souhaitée »")
                .hasMessageContaining("option inconnue « onclick »")
                .hasMessageContaining("cases de 80 à 260 px")
                .hasMessageContaining("chaque case a un libellé");
    }

    @Test
    void tokensAreReplacedAndUnknownTokensBecomeEmpty() {
        DocumentData data = new DocumentData("X").number("CSM-1").amount(new BigDecimal("118000"), "XAF").field("motif", "Motif", "Ramettes");
        assertThat(TemplateRenderer.tokens("{{motif}} ({{numero}}, {{montant}}){{inconnu}}", data)).isEqualTo("Ramettes (CSM-1, 118 000 XAF)");
        // Un jeton n'est jamais interprete : sa valeur reste du texte.
        DocumentData hostile = new DocumentData("X").field("motif", "Motif", "${T(java.lang.Runtime)} $1 \\");
        assertThat(TemplateRenderer.tokens("{{motif}}", hostile)).isEqualTo("${T(java.lang.Runtime)} $1 \\");
    }

    @Test
    void voucherKeysDeriveFromLabels() {
        VoucherService.Section first = new VoucherService.Section("Demande").pair("Date souhaitée", "2026-11-14").pair("Pièce d'identité", "CNI");
        VoucherService.Section second = new VoucherService.Section("Décision").pair("Date souhaitée", "autre");
        VoucherService.View view = new VoucherService.View("T", "N", null, null, false, "", null, List.of(first, second), null, null, "Je reconnais",
                List.of(new VoucherService.Signature("Niveau 2 : APPROVED", "R. Ibara", "11/11/2026")), "", "", "", "", null, null);

        DocumentData data = DocumentData.fromVoucher("FUND_REQUEST", view, null);

        assertThat(data.fields()).containsKeys("date_souhaitee", "piece_d_identite", "decision_date_souhaitee", "engagement");
        assertThat(data.signers()).containsKey("niveau_2");
    }

    @Test
    void publishedTemplateWinsOverTheOriginalVoucherTemplate() throws Exception {
        VoucherService vouchers = new VoucherService(null, null, new QrCodeRenderer());
        DocumentTemplatePrinter printer = mock(DocumentTemplatePrinter.class);
        when(printer.tryRender(eq("FUND_REQUEST"), any())).thenReturn(Optional.of(new byte[]{1, 2, 3}));
        Method setter = VoucherService.class.getDeclaredMethod("setTemplatePrinter", DocumentTemplatePrinter.class);
        setter.setAccessible(true);
        setter.invoke(vouchers, printer);

        VoucherService.Issued issued = new VoucherService.Issued("DDF-1", "DDF", "DDF-1", BigDecimal.TEN, "x", "me", Instant.now(), 0, null, null);
        assertThat(vouchers.render("Demande de fonds", issued, List.of(), BigDecimal.TEN, null, List.of())).containsExactly(1, 2, 3);
    }

    @Test
    void cashTemplateFollowsTheMovementType() {
        CashMovementResponse out = mock(CashMovementResponse.class);
        when(out.movementType()).thenReturn(CashMovementType.CASH_OUT);
        CashMovementResponse in = mock(CashMovementResponse.class);
        when(in.movementType()).thenReturn(CashMovementType.CASH_IN);
        CashMovementResponse payment = mock(CashMovementResponse.class);
        when(payment.movementType()).thenReturn(CashMovementType.PAYMENT);

        assertThat(CashVoucherDocumentData.templateCode(out)).isEqualTo("CASH_OUT_VOUCHER");
        assertThat(CashVoucherDocumentData.templateCode(in)).isEqualTo("CASH_IN_VOUCHER");
        assertThat(CashVoucherDocumentData.templateCode(payment)).isNull();
    }
}
