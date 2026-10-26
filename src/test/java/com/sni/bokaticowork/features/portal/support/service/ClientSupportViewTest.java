package com.sni.bokaticowork.features.portal.support.service;

import com.sni.bokaticowork.features.support.dto.SupportDtos.AttachmentResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.SupportTicketResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.TicketMessageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Le client ne voit ni les notes internes de l'equipe, ni l'agent assigne, ni le motif d'escalade. */
class ClientSupportViewTest {

    @Test
    @DisplayName("Les messages et pieces jointes internes sont retires, le reste est garde")
    void internalContentIsRemoved() {
        Instant now = Instant.now();
        SupportTicketResponse staffView = new SupportTicketResponse(
                "TCK-1", "Wifi", "Coupe", null, null, null, "MEMBER", "MBR-1", "Joel", "j@x.com", null,
                42L, null, null, null, null, null, null, null, null, null, null, 1, now, "client difficile",
                now, now,
                List.of(new TicketMessageResponse(1L, null, "MBR-1", "Joel", "Le wifi coupe", false, now),
                        new TicketMessageResponse(2L, null, "AG-7", "Agent", "Client deja relance trois fois", true, now)),
                List.of(new AttachmentResponse(1L, "capture.png", "image/png", 10L, false, null, now),
                        new AttachmentResponse(2L, "note-interne.pdf", "application/pdf", 10L, true, 7L, now)),
                List.of("wifi"));

        SupportTicketResponse clientView = ClientSupportService.clientView(staffView);

        assertThat(clientView.messages()).extracting(TicketMessageResponse::id).containsExactly(1L);
        assertThat(clientView.attachments()).extracting(AttachmentResponse::id).containsExactly(1L);
        assertThat(clientView.assignedTo()).isNull();
        assertThat(clientView.escalationReason()).isNull();
        assertThat(clientView.title()).isEqualTo("Wifi");
        assertThat(clientView.tags()).containsExactly("wifi");
    }
}
