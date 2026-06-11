package com.sni.bokaticowork.features.support.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.*;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.task.dto.TaskDtos.TaskResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface SupportTicketService {
    SupportTicketResponse create(CreateTicketRequest request);
    SupportTicketResponse createFromAutomation(CreateTicketRequest request);
    PaginatedResponse<SupportTicketResponse> search(TicketStatus status, Long assignedTo,
                                                    String ownerType, String ownerCode,
                                                    TicketCategory category, TicketPriority priority,
                                                    String relatedType, String relatedCode,
                                                    Boolean overdueOnly,
                                                    String searchText, Pageable pageable);
    SupportTicketResponse get(String ticketNumber);
    SupportTicketResponse assign(String ticketNumber, AssignTicketRequest request);
    SupportTicketResponse updateStatus(String ticketNumber, UpdateTicketStatusRequest request);
    SupportTicketResponse addMessage(String ticketNumber, AddTicketMessageRequest request);
    SupportTicketResponse close(String ticketNumber);
    SupportTicketResponse submitCsat(String ticketNumber, SubmitCsatRequest request);
    SupportTicketResponse addEmailReply(String ticketNumber, String graphMessageId,
                                        String senderEmail, String senderName, String content);
    SupportTicketResponse addTag(String ticketNumber, AddTagRequest request);
    SupportTicketResponse removeTag(String ticketNumber, String tag);
    List<TagResponse> listTags();
    List<TicketEventResponse> getTimeline(String ticketNumber);
    TaskResponse createTask(String ticketNumber, CreateTicketTaskRequest request);
    SupportMetricsResponse metrics();
    SupportAnalyticsResponse analytics(Instant from, Instant to);
    String analyticsCsv(Instant from, Instant to);
    List<AgentPerformanceEntry> agentPerformance(Instant from, Instant to);
    OwnerTicketSummaryResponse ownerSummary(String ownerType, String ownerCode);
}
