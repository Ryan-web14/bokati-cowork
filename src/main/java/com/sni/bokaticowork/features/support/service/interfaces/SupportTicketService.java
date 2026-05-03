package com.sni.bokaticowork.features.support.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import org.springframework.data.domain.Pageable;

public interface SupportTicketService {
    SupportTicketResponse create(CreateTicketRequest request);
    PaginatedResponse<SupportTicketResponse> search(TicketStatus status, Long assignedTo, String ownerType, String ownerCode, Pageable pageable);
    SupportTicketResponse get(String ticketNumber);
    SupportTicketResponse assign(String ticketNumber, AssignTicketRequest request);
    SupportTicketResponse updateStatus(String ticketNumber, UpdateTicketStatusRequest request);
    SupportTicketResponse addMessage(String ticketNumber, AddTicketMessageRequest request);
    SupportTicketResponse close(String ticketNumber);
    SupportMetricsResponse metrics();
}
