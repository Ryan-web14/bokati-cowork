package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.documentMaster.service.implementation.DocumentStorageService;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.model.TicketAttachment;
import com.sni.bokaticowork.features.support.model.TicketMessage;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.repository.TicketAttachmentRepository;
import com.sni.bokaticowork.features.support.repository.TicketMessageRepository;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SupportAttachmentService {

    private static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv"
    );

    private final SupportTicketRepository ticketRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final TicketMessageRepository messageRepository;
    private final DocumentStorageService storageService;

    public TicketAttachment upload(String ticketNumber, MultipartFile file, boolean internal, Long messageId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier joint est requis");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("Le fichier dépasse la taille maximale autorisée (15 Mo)");
        }
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
        if (!ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Type de fichier non autorisé : " + contentType);
        }

        SupportTicket ticket = getTicket(ticketNumber);
        TicketMessage message = resolveMessage(ticket, messageId);
        String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "fichier";
        String documentCode = "ATT-" + ticket.getTicketNumber() + "-" + Instant.now().toEpochMilli();
        DocumentStorageService.StoredDocument stored = storageService.store(
                "support/" + ticket.getTicketNumber(), documentCode, 1, file);

        TicketAttachment attachment = TicketAttachment.builder()
                .ticket(ticket)
                .message(message)
                .documentCode(documentCode)
                .fileName(fileName)
                .storageProvider(stored.provider())
                .storagePath(stored.storagePath())
                .contentType(contentType)
                .fileSize(file.getSize())
                .internal(internal)
                .uploadedBy(currentUserId())
                .active(true)
                .build();
        return attachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public List<TicketAttachment> list(String ticketNumber, boolean includeInternal) {
        SupportTicket ticket = getTicket(ticketNumber);
        List<TicketAttachment> attachments = attachmentRepository.findAllByTicketAndActiveTrueOrderByCreatedAtAsc(ticket);
        return includeInternal ? attachments
                : attachments.stream().filter(a -> !Boolean.TRUE.equals(a.getInternal())).toList();
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment download(String ticketNumber, Long attachmentId, boolean includeInternal) {
        TicketAttachment attachment = getAttachment(ticketNumber, attachmentId);
        if (!includeInternal && Boolean.TRUE.equals(attachment.getInternal())) {
            throw new ResourceNotFoundException("Pièce jointe introuvable : " + attachmentId);
        }
        byte[] content = storageService.read(attachment.getStorageProvider(), attachment.getStoragePath());
        return new DownloadedAttachment(attachment.getFileName(), attachment.getContentType(), content);
    }

    public void delete(String ticketNumber, Long attachmentId) {
        TicketAttachment attachment = getAttachment(ticketNumber, attachmentId);
        attachment.setActive(false);
        attachmentRepository.save(attachment);
    }

    // ── Helpers ───────────────────────────────────────────────────

    private SupportTicket getTicket(String ticketNumber) {
        if (!StringUtils.hasText(ticketNumber)) throw new BadRequestException("Le numéro de ticket est requis");
        return ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket de support introuvable : " + ticketNumber));
    }

    private TicketAttachment getAttachment(String ticketNumber, Long attachmentId) {
        SupportTicket ticket = getTicket(ticketNumber);
        return attachmentRepository.findByIdAndTicket(attachmentId, ticket)
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable : " + attachmentId));
    }

    private TicketMessage resolveMessage(SupportTicket ticket, Long messageId) {
        if (messageId == null) return null;
        TicketMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message introuvable : " + messageId));
        if (!message.getTicket().getId().equals(ticket.getId())) {
            throw new BadRequestException("Le message ne correspond pas à ce ticket");
        }
        return message;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return null;
    }

    public record DownloadedAttachment(String fileName, String contentType, byte[] content) {}
}
