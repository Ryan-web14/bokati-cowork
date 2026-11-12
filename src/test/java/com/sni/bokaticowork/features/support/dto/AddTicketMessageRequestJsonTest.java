package com.sni.bokaticowork.features.support.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le corps qu'envoie l'ecran du support, {@code authorId} et {@code authorName} compris, se lit
 * sans erreur avec Jackson 3 et les proprietes inconnues refusees, comme sur l'API.
 */
class AddTicketMessageRequestJsonTest {

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    @Test
    @DisplayName("Le corps du back-office se lit, senderId l'emporte sur authorId")
    void backOfficeBodyIsRead() {
        SupportDtos.AddTicketMessageRequest request = JSON.readValue("""
                {"senderType":"AGENT","senderId":"support@sni-cg.com","senderName":"support@sni-cg.com",
                 "authorId":"support@sni-cg.com","authorName":"support@sni-cg.com","content":"<p>bonjour</p>","internal":false}
                """, SupportDtos.AddTicketMessageRequest.class);

        assertThat(request.content()).isEqualTo("<p>bonjour</p>");
        assertThat(request.effectiveSenderId()).isEqualTo("support@sni-cg.com");

        SupportDtos.AddTicketMessageRequest authorOnly = JSON.readValue("""
                {"authorId":"agent@x.cg","authorName":"Agent X","content":"ok"}
                """, SupportDtos.AddTicketMessageRequest.class);
        assertThat(authorOnly.effectiveSenderId()).isEqualTo("agent@x.cg");
        assertThat(authorOnly.effectiveSenderName()).isEqualTo("Agent X");
    }
}
