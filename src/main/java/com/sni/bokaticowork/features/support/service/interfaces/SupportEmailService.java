package com.sni.bokaticowork.features.support.service.interfaces;

import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketMessage;

public interface SupportEmailService {
    void sendTicketCreated(SupportTicket ticket);
    void sendAgentMessage(SupportTicket ticket, TicketMessage message);
    void sendClientMessage(SupportTicket ticket, TicketMessage message);
    void sendTicketResolved(SupportTicket ticket);
    void sendTicketReopened(SupportTicket ticket);
    void sendTicketReopenedToAgent(SupportTicket ticket, TicketMessage message);
    void sendSlaBreachAlert(SupportTicket ticket, String breachType);
    void sendEscalationAlert(SupportTicket ticket, int level, String reason);
    void sendCsatRequest(SupportTicket ticket);
}
