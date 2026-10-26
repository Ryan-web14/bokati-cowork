package com.sni.bokaticowork.features.payment.dto.request;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Le front envoie createdBy · il est accepte et ignore, les autres inconnus restent refuses. */
class CreatePaymentIntentRequestJsonTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Test
    @DisplayName("createdBy envoye par le front est accepte, et ignore")
    void createdByIsAcceptedAndIgnored() throws Exception {
        CreatePaymentIntentRequest request = mapper.readValue("""
                {"customerType":"MEMBER","customerCode":"MBR-1","amount":1,"currency":"XAF",
                 "purpose":"MANUAL_PAYMENT","sourceType":"MANUAL","createdBy":"support@sni-cg.com"}
                """, CreatePaymentIntentRequest.class);

        assertThat(request.amount()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Une autre propriete inconnue reste refusee")
    void otherUnknownPropertiesAreStillRefused() {
        assertThatThrownBy(() -> mapper.readValue("""
                {"customerType":"MEMBER","customerCode":"MBR-1","amount":1,"currency":"XAF","approvedBy":"x"}
                """, CreatePaymentIntentRequest.class)).isInstanceOf(UnrecognizedPropertyException.class);
    }
}
