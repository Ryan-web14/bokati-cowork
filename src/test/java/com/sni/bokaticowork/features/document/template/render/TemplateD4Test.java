package com.sni.bokaticowork.features.document.template.render;

import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot D4 : capture des donnees d'une piece pour l'apercu, sections dans l'ordre de la piece. */
class TemplateD4Test {

    @Test
    void captureKeepsTheDataInsteadOfThePdf() {
        VoucherService vouchers = new VoucherService(null, null, new QrCodeRenderer());
        VoucherService.Issued issued = new VoucherService.Issued("BRB-1", "BRB", "SLIP-1", BigDecimal.TEN, "x", "me", Instant.now(), 0, null, null);
        VoucherService.Section first = new VoucherService.Section("Remise").pair("Bordereau", "BRB-1").pair("Caisse", "G1");
        VoucherService.Section cheques = new VoucherService.Section("Chèques").columns(List.of("N° chèque", "Montant"), List.of(false, true));
        cheques.row("0001", "10");
        byte[][] produced = new byte[1][];

        DocumentData data = VoucherService.capturing(() ->
                produced[0] = vouchers.render("Bordereau", issued, List.of(first, cheques), BigDecimal.TEN, null, List.of()));

        assertThat(produced[0]).isEmpty();
        assertThat(data.code()).isEqualTo("BANK_DEPOSIT_SLIP");
        assertThat(data.sourceCode()).isEqualTo("SLIP-1");
        assertThat(data.sections()).extracting(DocumentData.Section::key).containsExactly("remise", "cheques");
        assertThat(data.sections().get(0).fieldKeys()).containsExactly("bordereau", "caisse");
        assertThat(data.sections().get(1).tableKey()).isEqualTo("cheques");
    }
}
