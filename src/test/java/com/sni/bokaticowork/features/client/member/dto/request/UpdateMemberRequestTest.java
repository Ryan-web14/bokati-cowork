package com.sni.bokaticowork.features.client.member.dto.request;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La fiche membre lue puis renvoyee a la modification passe ; une cle mal orthographiee reste refusee. */
class UpdateMemberRequestTest {

    private final JsonMapper mapper = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    @Test
    void theMemberAsReadCanBeSentBack() {
        UpdateMemberRequest request = mapper.readValue("""
                {"memberId": "MBR-202607-00000005", "customerId": "CUS-PER-202607-00000005", "userId": "USR-100726-W9RB3P4V",
                 "firstname": "Obiang", "lastname": "NGUEMA", "fullname": "Obiang NGUEMA", "email": "membre@example.com",
                 "phone": "066950431", "whatsappPhone": "+242066589692", "status": "ACTIVE", "portalAccess": true,
                 "createdAt": "2026-07-10T01:47:53.549177Z", "updatedAt": "2026-07-10T02:11:55.624768Z"}
                """, UpdateMemberRequest.class);
        assertThat(request.getFirstname()).isEqualTo("Obiang");
        assertThat(request.getWhatsappPhone()).isEqualTo("+242066589692");
    }

    @Test
    void aMisspelledKeyIsStillRefused() {
        assertThatThrownBy(() -> mapper.readValue("{\"fistname\": \"Obiang\"}", UpdateMemberRequest.class))
                .hasMessageContaining("fistname");
    }
}
