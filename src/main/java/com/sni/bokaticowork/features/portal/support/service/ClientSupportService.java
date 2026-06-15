package com.sni.bokaticowork.features.portal.support.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.support.dto.request.ClientAddMessageRequest;
import com.sni.bokaticowork.features.portal.support.dto.request.ClientCreateTicketRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.AddTicketMessageRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.AttachmentResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.CreateTicketRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.OwnerTicketSummaryResponse;
import com.sni.bokaticowork.features.support.dto.SupportDtos.SubmitCsatRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.SupportTicketResponse;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketSenderType;
import com.sni.bokaticowork.features.support.enums.TicketStatus;
import com.sni.bokaticowork.features.support.mapper.SupportTicketMapper;
import com.sni.bokaticowork.features.support.service.implementation.SupportAttachmentService;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientSupportService {

    private static final String OWNER_TYPE = "MEMBER";

    private final SupportTicketService ticketService;
    private final SupportAttachmentService attachmentService;
    private final SupportTicketMapper mapper;

    @Transactional
    public SupportTicketResponse createTicket(Member member, ClientCreateTicketRequest request) {
        CreateTicketRequest internal = new CreateTicketRequest(
                request.title(),
                request.description(),
                request.priority(),
                request.category(),
                OWNER_TYPE,
                member.getMemberId(),
                member.getDisplayName(),
                member.getEmail(),
                member.getPhone(),
                request.relatedType(),
                request.relatedCode()
        );
        return ticketService.create(internal);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<SupportTicketResponse> listMyTickets(Member member,
                                                                   TicketStatus status,
                                                                   TicketCategory category,
                                                                   String searchText,
                                                                   Pageable pageable) {
        return ticketService.search(status, null, OWNER_TYPE, member.getMemberId(),
                category, null, null, null, null, searchText, pageable);
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getTicket(Member member, String ticketNumber) {
        SupportTicketResponse ticket = ticketService.get(ticketNumber);
        verifyOwnership(member, ticket);
        return ticket;
    }

    @Transactional
    public SupportTicketResponse addMessage(Member member, String ticketNumber,
                                             ClientAddMessageRequest request) {
        SupportTicketResponse ticket = ticketService.get(ticketNumber);
        verifyOwnership(member, ticket);
        AddTicketMessageRequest msg = new AddTicketMessageRequest(
                TicketSenderType.CLIENT,
                member.getMemberId(),
                member.getDisplayName(),
                request.content(),
                false
        );
        return ticketService.addMessage(ticketNumber, msg);
    }

    @Transactional
    public AttachmentResponse uploadAttachment(Member member, String ticketNumber, MultipartFile file) {
        SupportTicketResponse ticket = ticketService.get(ticketNumber);
        verifyOwnership(member, ticket);
        var attachment = attachmentService.upload(ticketNumber, file, false, null);
        return mapper.toAttachmentResponse(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachments(Member member, String ticketNumber) {
        SupportTicketResponse ticket = ticketService.get(ticketNumber);
        verifyOwnership(member, ticket);
        return attachmentService.list(ticketNumber, false).stream()
                .map(mapper::toAttachmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportAttachmentService.DownloadedAttachment downloadAttachment(Member member,
                                                                             String ticketNumber,
                                                                             Long id) {
        SupportTicketResponse ticket = ticketService.get(ticketNumber);
        verifyOwnership(member, ticket);
        return attachmentService.download(ticketNumber, id, false);
    }

    @Transactional
    public SupportTicketResponse closeTicket(Member member, String ticketNumber) {
        SupportTicketResponse ticket = ticketService.get(ticketNumber);
        verifyOwnership(member, ticket);
        return ticketService.close(ticketNumber);
    }

    @Transactional
    public SupportTicketResponse submitCsat(Member member, String ticketNumber,
                                             SubmitCsatRequest request) {
        SupportTicketResponse ticket = ticketService.get(ticketNumber);
        verifyOwnership(member, ticket);
        return ticketService.submitCsat(ticketNumber, request);
    }

    @Transactional(readOnly = true)
    public OwnerTicketSummaryResponse getSummary(Member member) {
        return ticketService.ownerSummary(OWNER_TYPE, member.getMemberId());
    }

    private void verifyOwnership(Member member, SupportTicketResponse ticket) {
        if (!OWNER_TYPE.equals(ticket.ownerType())
                || !member.getMemberId().equals(ticket.ownerCode())) {
            throw new ResourceNotFoundException("Ticket not found");
        }
    }
}
