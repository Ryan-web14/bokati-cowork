package com.sni.bokaticowork.features.billing.service.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La somme en lettres est la mention qui rend une facture difficile a raturer. Une orthographe
 * fautive la decredibilise autant qu'un chiffre faux, et le francais reserve plusieurs pieges.
 */
class FrenchAmountWordsTest {

    @ParameterizedTest
    @CsvSource({
            "0, zéro",
            "1, un",
            "16, seize",
            "17, dix-sept",
            "20, vingt",
            "21, vingt et un",
            "22, vingt-deux",
            "31, trente et un",
            "60, soixante",
            "70, soixante-dix",
            "71, soixante et onze",
            "72, soixante-douze",
            "79, soixante-dix-neuf",
            "80, quatre-vingts",
            "81, quatre-vingt-un",
            "82, quatre-vingt-deux",
            "90, quatre-vingt-dix",
            "91, quatre-vingt-onze",
            "99, quatre-vingt-dix-neuf"
    })
    void shouldSpellTheTensThatTripFrenchUp(long value, String expected) {
        assertThat(FrenchAmountWords.of(BigDecimal.valueOf(value))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "100, cent",
            "101, cent un",
            "180, cent quatre-vingts",
            "200, deux cents",
            "201, deux cent un",
            "999, neuf cent quatre-vingt-dix-neuf"
    })
    void shouldDropTheSOfCentWhenANumberFollows(long value, String expected) {
        assertThat(FrenchAmountWords.of(BigDecimal.valueOf(value))).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "1000, mille",
            "1001, mille un",
            "2000, deux mille",
            "80000, quatre-vingt mille",
            "200000, deux cent mille",
            "1000000, un million",
            "2000000, deux millions",
            "1000000000, un milliard"
    })
    void shouldKeepMilleInvariableAndPluraliseMillion(long value, String expected) {
        // « deux milles » est une faute courante · mille ne s'accorde jamais.
        assertThat(FrenchAmountWords.of(BigDecimal.valueOf(value))).isEqualTo(expected);
    }

    @Test
    void shouldSpellTheAmountOfTheReferenceInvoice() {
        assertThat(FrenchAmountWords.of(new BigDecimal("547653")))
                .isEqualTo("cinq cent quarante-sept mille six cent cinquante-trois");
    }

    @Test
    void shouldRoundToTheWholeFranc() {
        // Le franc CFA n'a pas de subdivision en circulation · imprimer des centimes en lettres
        // laisserait croire le contraire.
        assertThat(FrenchAmountWords.of(new BigDecimal("4145.40"))).isEqualTo("quatre mille cent quarante-cinq");
        assertThat(FrenchAmountWords.of(new BigDecimal("4145.60"))).isEqualTo("quatre mille cent quarante-six");
    }

    @Test
    void shouldAppendTheCurrencyInWords() {
        assertThat(FrenchAmountWords.withCurrency(new BigDecimal("547653"), "XAF"))
                .isEqualTo("cinq cent quarante-sept mille six cent cinquante-trois francs CFA");
        assertThat(FrenchAmountWords.withCurrency(new BigDecimal("100"), "EUR"))
                .isEqualTo("cent EUR");
    }

    @Test
    void shouldHandleNullAndNegative() {
        assertThat(FrenchAmountWords.of(null)).isNull();
        assertThat(FrenchAmountWords.of(new BigDecimal("-50"))).isEqualTo("moins cinquante");
    }
}
