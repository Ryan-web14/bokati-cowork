package com.sni.bokaticowork.features.support;

import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.repository.SupportTicketTagRepository;
import com.sni.bokaticowork.features.support.repository.TicketAttachmentRepository;
import com.sni.bokaticowork.features.support.repository.TicketMessageRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les requetes qui protegent la memoire · un fil sans le contenu demesure, des comptes par ticket.
 */
class SupportTicketListMemoryIT extends PostgresIntegrationTestBase {

    @Autowired private EntityManager entityManager;
    @Autowired private SupportTicketRepository tickets;
    @Autowired private TicketMessageRepository messages;
    @Autowired private TicketAttachmentRepository attachments;
    @Autowired private SupportTicketTagRepository tags;

    private SupportTicket ticket() {
        entityManager.createNativeQuery("""
                INSERT INTO support_ticket (ticket_number, title, status, priority, category, created_at, updated_at)
                VALUES (:number, 'Wifi', 'OPEN', 'MEDIUM', 'OTHER', now(), now())
                """).setParameter("number", "TCK-" + UUID.randomUUID()).executeUpdate();
        return tickets.findAll().stream().reduce((first, second) -> second).orElseThrow();
    }

    private void message(SupportTicket ticket, String content, boolean internal) {
        entityManager.createNativeQuery("""
                INSERT INTO ticket_message (ticket_id, sender_type, message, internal, created_at)
                VALUES (:ticket, 'AGENT', :content, :internal, now())
                """).setParameter("ticket", ticket.getId()).setParameter("content", content)
                .setParameter("internal", internal).executeUpdate();
    }

    @Test
    @DisplayName("Le fil revient sans le contenu d'un message demesure, sa longueur reste connue")
    void oversizedContentStaysInTheDatabase() {
        SupportTicket ticket = ticket();
        message(ticket, "Le wifi coupe", false);
        message(ticket, "x".repeat(300), false);

        List<TicketMessageRepository.MessageView> views = messages.findViewsByTicket(ticket.getId(), 200);

        assertThat(views).hasSize(2);
        assertThat(views).extracting(TicketMessageRepository.MessageView::content).containsExactlyInAnyOrder("Le wifi coupe", null);
        assertThat(views).extracting(TicketMessageRepository.MessageView::contentLength).containsExactlyInAnyOrder(13, 300);
        assertThat(views.getFirst().createdAt()).isNotNull();
    }

    @Test
    @DisplayName("Les comptes d'une page s'obtiennent en une requete, notes internes distinguees")
    void countsByTicket() {
        SupportTicket ticket = ticket();
        message(ticket, "Bonjour", false);
        message(ticket, "Note interne", true);

        List<Object[]> counts = messages.countByTickets(List.of(ticket.getId()));

        assertThat(counts).hasSize(1);
        assertThat(((Number) counts.getFirst()[1]).intValue()).isEqualTo(2);
        assertThat(((Number) counts.getFirst()[2]).intValue()).isEqualTo(1);
        assertThat(attachments.countActiveByTickets(List.of(ticket.getId()))).isEmpty();
        assertThat(tags.findAllByTicketIds(List.of(ticket.getId()))).isEmpty();
    }

    @Test
    @DisplayName("Le type de message reste lisible par la vue")
    void senderTypeIsRead() {
        SupportTicket ticket = ticket();
        message(ticket, "Bonjour", false);

        assertThat(messages.findViewsByTicket(ticket.getId(), 100).getFirst().senderType())
                .isEqualTo(com.sni.bokaticowork.features.support.enums.TicketSenderType.AGENT);
    }
}
