package com.sni.bokaticowork.features.support.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'ecran des tickets lit {@code assignedTo} comme un texte · un nombre le faisait planter
 * ({@code e.split is not a function}) des qu'un ticket etait assigne.
 */
class TicketAssignedToJsonTest {

    private static SupportDtos.SupportTicketResponse ticket(Long assignedTo) {
        return new SupportDtos.SupportTicketResponse("TCK-1", "Wifi", null, null, null, null, null, null, null, null, null,
                assignedTo, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), 0, 0, 0, 0);
    }

    @Test
    @DisplayName("assignedTo sort en texte, sans perdre les chiffres d'un long identifiant")
    void assignedToIsAString() {
        JsonNode json = new ObjectMapper().registerModule(new JavaTimeModule()).valueToTree(ticket(7291495199787880448L));

        assertThat(json.get("assignedTo").isTextual()).isTrue();
        assertThat(json.get("assignedTo").asText()).isEqualTo("7291495199787880448");
    }

    @Test
    @DisplayName("Un ticket non assigne reste vide")
    void unassignedStaysNull() {
        JsonNode json = new ObjectMapper().registerModule(new JavaTimeModule()).valueToTree(ticket(null));

        assertThat(json.get("assignedTo").isNull()).isTrue();
    }

    @Test
    @DisplayName("Les comptes visibles du client ne sortent pas dans le JSON")
    void visibleCountsAreNotExposed() {
        JsonNode json = new ObjectMapper().registerModule(new JavaTimeModule()).valueToTree(ticket(null));

        assertThat(json.has("messageCount")).isTrue();
        assertThat(json.has("visibleMessageCount")).isFalse();
    }
}
