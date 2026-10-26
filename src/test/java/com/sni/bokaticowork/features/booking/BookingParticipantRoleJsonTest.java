package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.features.booking.dto.request.BookingParticipantRequest;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le role d'un participant se lit tel que le portail l'envoie · Jackson 3, celui de l'application.
 *
 * <p>Le portail envoyait {@code "MEMBER"} pour un participant membre de l'espace, et toute la
 * reservation etait refusee en 400.</p>
 */
class BookingParticipantRoleJsonTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();

    private BookingParticipantRole read(String role) {
        return mapper.readValue("{\"name\":\"Kerker\",\"email\":\"admin@kerservice.com\",\"role\":" + role + "}",
                BookingParticipantRequest.class).role();
    }

    @Test
    @DisplayName("MEMBER se lit, comme les autres roles")
    void readsMember() {
        assertThat(read("\"MEMBER\"")).isEqualTo(BookingParticipantRole.MEMBER);
        assertThat(read("\"GUEST\"")).isEqualTo(BookingParticipantRole.GUEST);
    }

    @Test
    @DisplayName("La casse et les espaces ne comptent pas, un role vide revient au defaut")
    void isLenientOnCase() {
        assertThat(read("\" member \"")).isEqualTo(BookingParticipantRole.MEMBER);
        assertThat(read("\"\"")).isNull();
        assertThat(read("null")).isNull();
    }

    @Test
    @DisplayName("Un role inconnu est refuse en disant les valeurs acceptees")
    void rejectsAnUnknownRole() {
        assertThatThrownBy(() -> read("\"VIP\""))
                .hasMessageContaining("MEMBER")
                .hasMessageContaining("VIP");
    }
}
