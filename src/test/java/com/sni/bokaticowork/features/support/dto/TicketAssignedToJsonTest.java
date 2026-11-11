package com.sni.bokaticowork.features.support.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'ecran des tickets lit {@code assignedTo} comme un texte · un nombre le faisait planter
 * ({@code e.split is not a function}) des qu'un ticket etait assigne.
 *
 * <p>Les reponses HTTP passent par Jackson 3 : c'est lui qu'il faut interroger. Une annotation de
 * Jackson 2 passait ce test avec un {@code ObjectMapper} de Jackson 2, et restait sans effet sur
 * l'API.</p>
 */
class TicketAssignedToJsonTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static SupportDtos.SupportTicketResponse ticket(String assignedTo) {
        return new SupportDtos.SupportTicketResponse("TCK-1", "Wifi", null, null, null, null, null, null, null, null, null,
                assignedTo, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), 0, 0, 0, 0);
    }

    @Test
    @DisplayName("assignedTo sort en texte : le courriel de l'agent")
    void assignedToIsAString() {
        JsonNode json = JSON.valueToTree(ticket("support@sni-cg.com"));

        assertThat(json.get("assignedTo").isString()).isTrue();
        assertThat(json.get("assignedTo").asString()).isEqualTo("support@sni-cg.com");
        assertThat(json.has("assignedToEmail")).isFalse();
    }

    @Test
    @DisplayName("Un ticket non assigne reste vide")
    void unassignedStaysNull() {
        JsonNode json = JSON.valueToTree(ticket(null));

        assertThat(json.get("assignedTo").isNull()).isTrue();
    }

    @Test
    @DisplayName("Les comptes visibles du client ne sortent pas dans le JSON")
    void visibleCountsAreNotExposed() {
        JsonNode json = JSON.valueToTree(ticket(null));

        assertThat(json.has("messageCount")).isTrue();
        assertThat(json.has("visibleMessageCount")).isFalse();
    }
}
