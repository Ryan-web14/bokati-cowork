package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionEventRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionStatusHistoryRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriptionEventType;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import com.sni.bokaticowork.features.subscription.subscription.model.SubscriptionEvent;
import com.sni.bokaticowork.features.subscription.timeline.service.SubscriptionTimelineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Le contenu d'un evenement d'abonnement part toujours en JSON valide · la colonne est en jsonb.
 */
class SubscriptionEventWriterTest {

    private final SubscriptionEventRepository events = mock(SubscriptionEventRepository.class);
    private final SubscriptionEventWriter writer = new SubscriptionEventWriter(mock(SubscriptionStatusHistoryRepository.class),
            events, mock(SubscriptionTimelineService.class));

    @Test
    @DisplayName("Un contenu produit par JsonPayload est echappe : guillemets, barre oblique, saut de ligne")
    void payloadIsEscaped() throws Exception {
        String reason = "Motif \"urgent\" \\ ligne 1\nligne 2";
        writer.writeEvent(new Subscription(), SubscriptionEventType.GRACE_PERIOD_ENTERED,
                com.sni.bokaticowork.core.utils.json.JsonPayload.of("reason", reason, "suspensionInDays", 7));

        ArgumentCaptor<SubscriptionEvent> saved = ArgumentCaptor.forClass(SubscriptionEvent.class);
        verify(events).save(saved.capture());
        var json = new ObjectMapper().readTree(saved.getValue().getPayloadJson());
        assertThat(json.get("reason").asText()).isEqualTo(reason);
        assertThat(json.get("suspensionInDays").asInt()).isEqualTo(7);
    }

    @Test
    @DisplayName("Un contenu mal forme est refuse avant la base, avec un message clair ; vide, il reste vide")
    void malformedIsRefused() {
        assertThat(writer.jsonPayload("  ")).isNull();
        assertThat(writer.jsonPayload("{\"released\":true}")).isEqualTo("{\"released\":true}");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> writer.jsonPayload("Deposit hold WHL-1 captured"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("JsonPayload");
    }
}
