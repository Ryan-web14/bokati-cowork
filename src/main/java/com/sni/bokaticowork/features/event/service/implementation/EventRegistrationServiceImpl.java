package com.sni.bokaticowork.features.event.service.implementation;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.client.member.dto.request.CreateMemberRequest;
import com.sni.bokaticowork.features.client.member.dto.response.MemberResponse;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventRegistrationResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.EventSummaryResponse;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.PublicEventRegistrationRequest;
import com.sni.bokaticowork.features.event.dto.EventRegistrationDtos.RejectEventRegistrationRequest;
import com.sni.bokaticowork.features.event.enums.RegistrationStatus;
import com.sni.bokaticowork.features.event.model.Event;
import com.sni.bokaticowork.features.event.model.EventRegistration;
import com.sni.bokaticowork.features.event.repository.EventRegistrationRepository;
import com.sni.bokaticowork.features.event.repository.EventRepository;
import com.sni.bokaticowork.features.event.service.interfaces.EventRegistrationService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.util.Locale;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class EventRegistrationServiceImpl implements EventRegistrationService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final MemberService memberService;
    private final DefaultEmailSender emailSender;
    private final SpringTemplateEngine emailTemplateEngine;

    @Value("${app.event.notification-email:coworkspace@elleaose.com}")
    private String notificationEmail;

    @Override
    @Transactional(readOnly = true)
    public EventSummaryResponse getEvent(String eventCode) {
        return summary(getActiveEvent(eventCode));
    }

    @Override
    public EventRegistrationResponse register(String eventCode, PublicEventRegistrationRequest request) {
        Event event = getActiveEvent(eventCode);

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        registrationRepository.findByEventIdAndEmailIgnoreCaseAndStatusNot(
                        event.getId(), email, RegistrationStatus.REJECTED)
                .ifPresent(existing -> {
                    throw new ResourceAlreadyExistException(
                            "Vous êtes déjà inscrit(e) à cet événement avec cet email");
                });

        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .firstname(request.firstname().trim())
                .lastname(request.lastname().trim())
                .email(email)
                .phone(request.phone().trim())
                .whatsappPhone(StringUtils.hasText(request.whatsappPhone())
                        ? request.whatsappPhone().trim() : request.phone().trim())
                .status(RegistrationStatus.PENDING_VALIDATION)
                .build();

        EventRegistration saved = registrationRepository.save(registration);
        sendRegistrationEmailsAfterCommit(saved, event);
        return response(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<EventRegistrationResponse> search(String eventCode, RegistrationStatus status,
                                                                 String searchText, Pageable pageable) {
        Long eventId = null;
        if (StringUtils.hasText(eventCode)) {
            eventId = eventRepository.findByCodeIgnoreCase(eventCode.trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode))
                    .getId();
        }
        String statusStr = status != null ? status.name() : null;
        String text = StringUtils.hasText(searchText) ? searchText.trim() : null;
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<EventRegistration> page = registrationRepository.search(eventId, statusStr, text, unsortedPageable);
        return new PaginatedResponse<>(page.map(this::response));
    }

    @Override
    @Transactional(readOnly = true)
    public EventRegistrationResponse get(Long id) {
        return response(getRegistration(id));
    }

    @Override
    public EventRegistrationResponse validate(Long id) {
        EventRegistration registration = getRegistration(id);
        if (registration.getStatus() != RegistrationStatus.PENDING_VALIDATION) {
            throw new BadRequestException("This registration has already been " + registration.getStatus());
        }

        CreateMemberRequest memberRequest = CreateMemberRequest.builder()
                .customerType("PERSON")
                .firstname(registration.getFirstname())
                .lastname(registration.getLastname())
                .email(registration.getEmail())
                .phone(registration.getPhone())
                .whatsappPhone(registration.getWhatsappPhone())
                .generatePassword(true)
                .build();

        MemberResponse member = memberService.create(memberRequest, true);

        registration.setStatus(RegistrationStatus.VALIDATED);
        registration.setMemberId(member.getMemberId());
        registration.setCustomerId(member.getCustomerId());
        registration.setValidatedBy(currentUserId());
        registration.setValidatedAt(Instant.now());

        return response(registrationRepository.save(registration));
    }

    @Override
    public EventRegistrationResponse reject(Long id, RejectEventRegistrationRequest request) {
        EventRegistration registration = getRegistration(id);
        if (registration.getStatus() != RegistrationStatus.PENDING_VALIDATION) {
            throw new BadRequestException("This registration has already been " + registration.getStatus());
        }
        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setRejectionReason(request != null ? request.reason() : null);
        return response(registrationRepository.save(registration));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Event getActiveEvent(String eventCode) {
        if (!StringUtils.hasText(eventCode)) {
            throw new BadRequestException("Event code is required");
        }
        Event event = eventRepository.findByCodeIgnoreCase(eventCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
        if (!event.isActive()) {
            throw new BadRequestException("Event is not open for registration: " + eventCode);
        }
        return event;
    }

    private EventRegistration getRegistration(Long id) {
        if (id == null) throw new BadRequestException("Registration id is required");
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found: " + id));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUser().getId();
        }
        return null;
    }

    private EventSummaryResponse summary(Event event) {
        return new EventSummaryResponse(
                event.getCode(), event.getName(), event.getDescription(),
                event.getEventDate(), event.isActive());
    }

    private EventRegistrationResponse response(EventRegistration r) {
        return new EventRegistrationResponse(
                r.getId(),
                r.getRegistrationNumber(),
                r.getEvent() != null ? r.getEvent().getCode() : null,
                r.getEvent() != null ? r.getEvent().getName() : null,
                r.getFirstname(),
                r.getLastname(),
                (r.getFirstname() + " " + r.getLastname()).trim(),
                r.getEmail(),
                r.getPhone(),
                r.getWhatsappPhone(),
                r.getStatus(),
                r.getRejectionReason(),
                r.getMemberId(),
                r.getCustomerId(),
                r.getCreatedAt(),
                r.getValidatedAt()
        );
    }

    private void sendRegistrationEmailsAfterCommit(EventRegistration registration, Event event) {
        Runnable task = () -> {
            String fullName = (registration.getFirstname() + " " + registration.getLastname()).trim();
            try {
                Context visitorCtx = new Context(Locale.FRENCH);
                visitorCtx.setVariable("audience", "VISITOR");
                visitorCtx.setVariable("fullName", fullName);
                visitorCtx.setVariable("eventName", event.getName());
                visitorCtx.setVariable("registrationNumber", registration.getRegistrationNumber());
                String visitorHtml = emailTemplateEngine.process("event-registration", visitorCtx);
                emailSender.queueEmail(registration.getEmail(),
                        "Merci pour votre confirmation — " + event.getName(),
                        visitorHtml, true, "EVENT_REGISTRATION", registration.getRegistrationNumber());
            } catch (Exception ex) {
                log.warn("Unable to queue thank-you email to {}", registration.getEmail(), ex);
            }

            if (!StringUtils.hasText(notificationEmail)) {
                return;
            }
            try {
                Context staffCtx = new Context(Locale.FRENCH);
                staffCtx.setVariable("audience", "STAFF");
                staffCtx.setVariable("fullName", fullName);
                staffCtx.setVariable("eventName", event.getName());
                staffCtx.setVariable("registrationNumber", registration.getRegistrationNumber());
                staffCtx.setVariable("email", registration.getEmail());
                staffCtx.setVariable("phone", registration.getPhone());
                staffCtx.setVariable("whatsappPhone", registration.getWhatsappPhone());
                String staffHtml = emailTemplateEngine.process("event-registration", staffCtx);
                emailSender.queueEmail(notificationEmail,
                        "Nouvelle confirmation — " + event.getName(),
                        staffHtml, true, "EVENT_REGISTRATION", registration.getRegistrationNumber());
            } catch (Exception ex) {
                log.warn("Unable to queue staff notification email to {}", notificationEmail, ex);
            }
        };
        runAfterCommit(task);
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }
}
