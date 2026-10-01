package com.sni.bokaticowork.features.payment.mobilemoney;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyInitiationGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ce qui se refuse avant d'appeler l'operateur se refuse en francais.
 *
 * <p>Chaque cas ici coutait auparavant au client une attente de plusieurs secondes suivie d'un
 * message technique de l'operateur · ou, pire, une demande partie sur le mauvais telephone.</p>
 */
class MobileMoneyInitiationGuardTest {

    private MobileMoneyInitiationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new MobileMoneyInitiationGuard();
        ReflectionTestUtils.setField(guard, "operatorPrefixes", "");
    }

    private PaymentIntent intent(String currency) {
        PaymentIntent intent = new PaymentIntent();
        intent.setIntentNumber("PIN-1");
        intent.setCurrency(currency);
        return intent;
    }

    private InitiateMobileMoneyDepositRequest request(String phone, CongoCorrespondent operator) {
        return new InitiateMobileMoneyDepositRequest("PIN-1", phone, operator, null, "test", null);
    }

    @Test
    @DisplayName("Un numero congolais bien forme avec le bon operateur passe")
    void acceptsAValidRequest() {
        assertThatCode(() -> guard.check(intent("XAF"), request("06 123 45 67", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("5000"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sans operateur on ne sait pas a qui envoyer la demande")
    void requiresAnOperator() {
        assertThatThrownBy(() -> guard.check(intent("XAF"), request("0612345 67", null), BigDecimal.TEN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("opérateur");
    }

    @Test
    @DisplayName("Un montant nul ou negatif n'est pas un paiement")
    void rejectsANonPositiveAmount() {
        assertThatThrownBy(() -> guard.check(intent("XAF"), request("0612345 67", CongoCorrespondent.MTN_MOMO_COG), BigDecimal.ZERO))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("positif");
        assertThatThrownBy(() -> guard.check(intent("XAF"), request("0612345 67", CongoCorrespondent.MTN_MOMO_COG), new BigDecimal("-10")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Les centimes ne passent pas · arrondir en silence changerait le montant du")
    void rejectsFractionalAmounts() {
        assertThatThrownBy(() -> guard.check(intent("XAF"), request("0612345 67", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("1500.50")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("entier");
    }

    @Test
    @DisplayName("Un montant entier ecrit avec des decimales nulles reste entier")
    void acceptsTrailingZeroScale() {
        assertThatCode(() -> guard.check(intent("XAF"), request("0612345 67", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("1500.00"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un operateur qui encaisse en CDF ne peut pas regler une facture en XAF")
    void rejectsACurrencyMismatch() {
        assertThatThrownBy(() -> guard.check(intent("XAF"), request("0812345 67", CongoCorrespondent.ORANGE_COD),
                new BigDecimal("5000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CDF")
                .hasMessageContaining("XAF");
    }

    @Test
    @DisplayName("Un numero illisible est refuse ici plutot que par l'operateur")
    void rejectsAnUnreadablePhoneNumber() {
        assertThatThrownBy(() -> guard.check(intent("XAF"), request("abc", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("5000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("numéro");
    }

    @Test
    @DisplayName("Avec des plages configurees, un numero d'un autre operateur est refuse tout de suite")
    void checksTheOperatorPrefixWhenConfigured() {
        ReflectionTestUtils.setField(guard, "operatorPrefixes", "MTN_MOMO_COG=06;AIRTEL_COG=04,05");

        assertThatCode(() -> guard.check(intent("XAF"), request("06 123 45 67", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("5000"))).doesNotThrowAnyException();

        assertThatThrownBy(() -> guard.check(intent("XAF"), request("04 123 45 67", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("5000")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("MTN Mobile Money Congo");
    }

    @Test
    @DisplayName("Sans plage configuree, aucun prefixe n'est refuse · une regle fausse bloquerait un vrai paiement")
    void doesNotCheckPrefixesWhenNoneAreConfigured() {
        assertThatCode(() -> guard.check(intent("XAF"), request("04 123 45 67", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("5000"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un numero deja international est confronte aux memes plages")
    void checksPrefixesOnInternationalNumbers() {
        ReflectionTestUtils.setField(guard, "operatorPrefixes", "MTN_MOMO_COG=06");
        assertThatCode(() -> guard.check(intent("XAF"), request("+242061234567", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("5000"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.check(intent("XAF"), request("+242051234567", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("5000")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Une intention sans devise ne bloque rien · le defaut se resout ailleurs")
    void toleratesAnIntentWithoutCurrency() {
        assertThat(intent(null).getCurrency()).isNull();
        assertThatCode(() -> guard.check(intent(null), request("06 123 45 67", CongoCorrespondent.MTN_MOMO_COG),
                new BigDecimal("5000"))).doesNotThrowAnyException();
    }
}
