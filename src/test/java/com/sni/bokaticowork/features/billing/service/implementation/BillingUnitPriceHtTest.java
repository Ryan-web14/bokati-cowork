package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La colonne « P.U. HT » doit etre hors taxe · elle affichait le prix total de la prestation.
 *
 * <p>{@code unitPrice} est le prix tel qu'il a ete saisi. Sur une ligne en prix TTC, c'est donc un
 * prix TTC, pendant que le brut de la meme ligne a ete ramene en HT : la facture montrait un
 * « P.U. HT » de 25 000 a cote d'un « Brut HT » de 21 186, et la ligne ne se verifiait plus de
 * gauche a droite.</p>
 */
class BillingUnitPriceHtTest {

    private final BillingDocumentPdfServiceImpl.BillingDocumentTemplateFormatter fmt =
            new BillingDocumentPdfServiceImpl.BillingDocumentTemplateFormatter(
                    "XAF", new ObjectMapper(), Locale.FRANCE);

    /**
     * @param quantity  quantite facturee
     * @param unitPrice prix unitaire tel que saisi · TTC si la ligne est en prix TTC
     * @param subtotal  brut hors taxe calcule par le moteur de facturation
     */
    private BillingDocumentLineResponse line(String quantity, String unitPrice, String subtotal) {
        return new BillingDocumentLineResponse(
                1, BillingLineType.SERVICE, null, null, "Salle de réunion", null,
                new BigDecimal(quantity), "h", new BigDecimal(unitPrice),
                null, null, true, null, new BigDecimal("18"), null,
                new BigDecimal(subtotal), null, null, null, null, null,
                null, null, null, null, false);
    }

    @Test
    @DisplayName("Ligne saisie hors taxe · le prix unitaire est celui saisi")
    void taxExcludedLineKeepsItsUnitPrice() {
        // 2 h x 10 000 HT = 20 000 HT
        assertThat(fmt.unitPriceHt(line("2", "10000", "20000")))
                .isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("Ligne saisie en prix TTC · le prix unitaire est ramene hors taxe")
    void taxIncludedLineIsBroughtBackToExcludingTax() {
        // 1 x 25 000 TTC · le moteur a calcule un brut HT de 25 000 / 1,18
        BigDecimal unitHt = fmt.unitPriceHt(line("1", "25000", "21186.4400"));

        assertThat(unitHt).isEqualByComparingTo(new BigDecimal("21186.44"));
        // Et surtout : plus le prix total de la prestation.
        assertThat(unitHt).isNotEqualByComparingTo(new BigDecimal("25000"));
    }

    @Test
    @DisplayName("Le prix unitaire multiplie par la quantite retombe sur le brut HT affiche a cote")
    void theLineReadsFromLeftToRight() {
        BillingDocumentLineResponse subject = line("3", "11800", "30000");

        assertThat(fmt.unitPriceHt(subject).multiply(subject.quantity()))
                .isEqualByComparingTo(subject.subtotalAmount());
    }

    @Test
    @DisplayName("Sans brut ni quantite, on rend ce qui a ete saisi plutot que zero")
    void fallsBackToTheCapturedPrice() {
        BillingDocumentLineResponse incomplete = new BillingDocumentLineResponse(
                1, BillingLineType.SERVICE, null, null, "Ligne incomplète", null,
                null, null, new BigDecimal("10000"),
                null, null, true, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, false);

        assertThat(fmt.unitPriceHt(incomplete)).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("Une quantite nulle ne fait pas diviser par zero")
    void neverDividesByZero() {
        assertThat(fmt.unitPriceHt(line("0", "10000", "0")))
                .isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("Une ligne absente rend zero")
    void aMissingLineIsZero() {
        assertThat(fmt.unitPriceHt(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
