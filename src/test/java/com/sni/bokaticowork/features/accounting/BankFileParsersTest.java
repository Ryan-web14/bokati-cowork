package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.features.accounting.service.support.BankFileParsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Lecture des releves OFX et MT940 (lot F10). */
class BankFileParsersTest {

    static final String OFX_SGML = """
            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102

            <OFX>
            <BANKMSGSRSV1><STMTTRNRS><STMTRS>
            <CURDEF>XAF
            <BANKTRANLIST>
            <DTSTART>20260401
            <DTEND>20260430
            <STMTTRN>
            <TRNTYPE>CREDIT
            <DTPOSTED>20260410120000
            <TRNAMT>250000.00
            <FITID>A1
            <NAME>REMISE BRB-202604-000001
            </STMTTRN>
            <STMTTRN>
            <TRNTYPE>DEBIT
            <DTPOSTED>20260415
            <TRNAMT>-5000.00
            <FITID>A2
            <NAME>FRAIS TENUE DE COMPTE
            <MEMO>AVRIL
            </STMTTRN>
            </BANKTRANLIST>
            <LEDGERBAL><BALAMT>1245000.00<DTASOF>20260430</LEDGERBAL>
            </STMTRS></STMTTRNRS></BANKMSGSRSV1>
            </OFX>
            """;

    static final String MT940 = """
            :20:STMT0426
            :25:30011/00012345678
            :28C:4/1
            :60F:C260401XAF1000000,00
            :61:2604100410C250000,00NTRFBRB-202604-000001//B1
            :86:REMISE ESPECES
            :61:2604150415D5000,00NCHGNONREF//B2
            :86:FRAIS TENUE
            DE COMPTE
            :62F:C260430XAF1245000,00
            -
            """;

    @Test
    @DisplayName("OFX SGML : operations, signes, references, soldes et periode")
    void ofx() {
        byte[] content = OFX_SGML.getBytes(StandardCharsets.ISO_8859_1);
        assertThat(BankFileParsers.detect(content)).isEqualTo("OFX");
        BankFileParsers.Parsed parsed = BankFileParsers.parse(content);
        assertThat(parsed.lines()).hasSize(2);
        assertThat(parsed.lines().get(0).amount()).isEqualByComparingTo("250000");
        assertThat(parsed.lines().get(0).date()).isEqualTo(LocalDate.of(2026, 4, 10));
        assertThat(parsed.lines().get(0).label()).isEqualTo("REMISE BRB-202604-000001");
        assertThat(parsed.lines().get(1).amount()).isEqualByComparingTo("-5000");
        assertThat(parsed.lines().get(1).label()).isEqualTo("FRAIS TENUE DE COMPTE AVRIL");
        assertThat(parsed.lines().get(1).reference()).isEqualTo("A2");
        assertThat(parsed.closingBalance()).isEqualByComparingTo("1245000");
        assertThat(parsed.openingBalance()).isEqualByComparingTo("1000000");
        assertThat(parsed.from()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(parsed.to()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("MT940 : :61: et :86:, credit et debit, references, soldes :60F: et :62F:")
    void mt940() {
        byte[] content = MT940.getBytes(StandardCharsets.ISO_8859_1);
        assertThat(BankFileParsers.detect(content)).isEqualTo("MT940");
        BankFileParsers.Parsed parsed = BankFileParsers.parse(content);
        assertThat(parsed.lines()).hasSize(2);
        assertThat(parsed.lines().get(0).amount()).isEqualByComparingTo("250000");
        assertThat(parsed.lines().get(0).reference()).isEqualTo("BRB-202604-000001");
        assertThat(parsed.lines().get(0).label()).isEqualTo("REMISE ESPECES");
        assertThat(parsed.lines().get(1).amount()).isEqualByComparingTo("-5000");
        assertThat(parsed.lines().get(1).reference()).isEqualTo("B2");
        assertThat(parsed.lines().get(1).label()).isEqualTo("FRAIS TENUE DE COMPTE");
        assertThat(parsed.openingBalance()).isEqualByComparingTo("1000000");
        assertThat(parsed.closingBalance()).isEqualByComparingTo("1245000");
        assertThat(parsed.to()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("Un CSV n'est reconnu ni comme OFX ni comme MT940")
    void csv() {
        assertThat(BankFileParsers.detect("Date;Libelle;Montant\n10/04/2026;X;1,00".getBytes(StandardCharsets.UTF_8))).isNull();
    }
}
