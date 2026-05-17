package com.sni.bokaticowork.features.support.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface SupportTicketService {
    SupportTicketResponse create(CreateTicketRequest request);
    PaginatedResponse<SupportTicketResponse> search(TicketStatus status, Long assignedTo,
                                                    String ownerType, String ownerCode,
                                                    String searchText, Pageable pageable);
    SupportTicketResponse get(String ticketNumber);
    SupportTicketResponse assign(String ticketNumber, AssignTicketRequest request);
    SupportTicketResponse updateStatus(String ticketNumber, UpdateTicketStatusRequest request);
    SupportTicketResponse addMessage(String ticketNumber, AddTicketMessageRequest request);
    SupportTicketResponse close(String ticketNumber);
    SupportTicketResponse submitCsat(String ticketNumber, SubmitCsatRequest request);
    SupportTicketResponse addEmailReply(String ticketNumber, String graphMessageId,
                                        String senderEmail, String senderName, String content);
    SupportMetricsResponse metrics();
    SupportAnalyticsResponse analytics(Instant from, Instant to);
}
