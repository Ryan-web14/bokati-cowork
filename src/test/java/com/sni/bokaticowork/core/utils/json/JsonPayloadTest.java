package com.sni.bokaticowork.core.utils.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les contenus JSON de la plateforme · echappes, dans l'ordre donne, dates ISO, montants en clair.
 */
class JsonPayloadTest {

    @Test
    @DisplayName("Les valeurs sont echappees et relues a l'identique")
    void escapes() throws Exception {
        String reason = "Motif \"urgent\" \\ ligne 1\nligne 2 · Annulé";
        String json = JsonPayload.of("reason", reason, "by", null);

        var tree = new ObjectMapper().readTree(json);
        assertThat(tree.get("reason").asText()).isEqualTo(reason);
        assertThat(tree.get("by").isNull()).isTrue();
        assertThat(JsonPayload.isValid(json)).isTrue();
        assertThat(JsonPayload.isValid("Deposit hold WHL-1 captured")).isFalse();
    }

    @Test
    @DisplayName("Ordre des cles garde, dates ISO, montants sans notation scientifique")
    void formats() {
        assertThat(JsonPayload.of("until", LocalDate.of(2026, 11, 2), "fee", new BigDecimal("1E+5"), "days", 3, "credit", true))
                .isEqualTo("{\"until\":\"2026-11-02\",\"fee\":100000,\"days\":3,\"credit\":true}");
    }

    @Test
    @DisplayName("QR fiscal : la meme chaine qu'avec l'ancien gabarit")
    void fiscalPayloadUnchanged() {
        String before = String.format("{\"n\":\"%s\",\"d\":\"%s\",\"t\":%s,\"h\":\"%s\",\"s\":\"%s\"}",
                "FAC20260617-00001abc-BKTWK", "2026-06-17", new BigDecimal("295000.0000").toPlainString(), "a1b2c3d4e5f6a7b8", "e5f6a7b8c9d0e1f2");
        String after = JsonPayload.of("n", "FAC20260617-00001abc-BKTWK", "d", "2026-06-17", "t", new BigDecimal("295000.0000"),
                "h", "a1b2c3d4e5f6a7b8", "s", "e5f6a7b8c9d0e1f2");
        assertThat(after).isEqualTo(before);
    }

    @Test
    @DisplayName("Paires incompletes ou cle non textuelle : refus")
    void refusesBadPairs() {
        assertThatThrownBy(() -> JsonPayload.of("a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonPayload.of(1, "a")).isInstanceOf(IllegalArgumentException.class);
    }
}
