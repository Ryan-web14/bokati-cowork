package com.sni.bokaticowork.features.support;

import com.sni.bokaticowork.core.richtext.RichTextSupport;
import com.sni.bokaticowork.features.support.dto.SupportDtos.AddTicketMessageRequest;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.repository.TicketMessageRepository;
import com.sni.bokaticowork.features.support.service.implementation.SupportTicketServiceImpl;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un message d'agent est signe par l'agent connecte quand l'ecran n'envoie ni {@code senderId} ni
 * {@code senderName} ; un expediteur fourni est garde.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SupportTicketMessageSenderTest {

    @Mock private SupportTicketRepository ticketRepository;
    @Mock private TicketMessageRepository messageRepository;
    @Mock private RichTextSupport richTextSupport;

    @InjectMocks private SupportTicketServiceImpl service;

    @BeforeEach
    void setUp() {
        SupportTicket ticket = SupportTicket.builder().ticketNumber("TCK-1").status(TicketStatus.OPEN).build();
        when(ticketRepository.findByTicketNumber("TCK-1")).thenReturn(Optional.of(ticket));
        when(messageRepository.save(any(TicketMessage.class))).thenAnswer(call -> call.getArgument(0));
        when(richTextSupport.normalize(anyString())).thenAnswer(call -> call.getArgument(0));
        Users agent = new Users();
        agent.setId(5L);
        agent.setEmail("support@sni-cg.com");
        agent.setFirstname("Grace");
        agent.setLastname("Mavoungou");
        UserPrincipal principal = new UserPrincipal();
        ReflectionTestUtils.setField(principal, "user", agent);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    private TicketMessage saved() {
        ArgumentCaptor<TicketMessage> message = ArgumentCaptor.forClass(TicketMessage.class);
        verify(messageRepository).save(message.capture());
        return message.getValue();
    }

    @Test
    @DisplayName("Sans expediteur dans le corps, le message porte l'e-mail et le nom de l'agent connecte")
    void agentSignsHisMessage() {
        try {
            service.addMessage("TCK-1", new AddTicketMessageRequest(null, null, null, "<p>hello</p>", false));
        } catch (RuntimeException ignored) {
            // la suite (courriel, diffusion) ne compte pas ici
        }
        assertThat(saved().getSenderId()).isEqualTo("support@sni-cg.com");
        assertThat(saved().getSenderName()).isEqualTo("Grace Mavoungou");
    }

    @Test
    @DisplayName("Un expediteur fourni par l'ecran est garde")
    void providedSenderIsKept() {
        try {
            service.addMessage("TCK-1", new AddTicketMessageRequest(null, "agent-7", "Equipe accueil", "<p>hello</p>", false));
        } catch (RuntimeException ignored) {
            // idem
        }
        assertThat(saved().getSenderId()).isEqualTo("agent-7");
        assertThat(saved().getSenderName()).isEqualTo("Equipe accueil");
    }

    @Test
    @DisplayName("authorId et authorName, envoyes par l'ecran, valent senderId et senderName")
    void authorFieldsAreAccepted() {
        try {
            service.addMessage("TCK-1", new AddTicketMessageRequest(null, null, null, "<p>bonjour</p>", false,
                    "support@sni-cg.com", "Equipe support"));
        } catch (RuntimeException ignored) {
            // idem
        }
        assertThat(saved().getSenderId()).isEqualTo("support@sni-cg.com");
        assertThat(saved().getSenderName()).isEqualTo("Equipe support");
    }
}
