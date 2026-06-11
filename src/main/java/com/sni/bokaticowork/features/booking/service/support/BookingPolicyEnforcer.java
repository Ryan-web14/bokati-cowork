package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.model.BookingAudiencePolicy;
import com.sni.bokaticowork.features.booking.model.BookingQuotaOverride;
import com.sni.bokaticowork.features.booking.repository.BookingAudiencePolicyRepository;
import com.sni.bokaticowork.features.booking.repository.BookingQuotaOverrideRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.support.dto.SupportDtos.CreateTicketRequest;
import com.sni.bokaticowork.features.support.enums.TicketCategory;
import com.sni.bokaticowork.features.support.enums.TicketPriority;
import com.sni.bokaticowork.features.support.service.interfaces.SupportTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingPolicyEnforcer {

    private final BookingAudiencePolicyRepository policyRepository;
    private final BookingQuotaOverrideRepository overrideRepository;
    private final BookingRepository bookingRepository;
    private final PlatformTransactionManager transactionManager;
    @Lazy private final SupportTicketService supportTicketService;

    public EffectivePolicy enforce(SubscriberType ownerType, String ownerCode, Resource resource, LocalDateTime startedAt, LocalDateTime endedAt) {
        BookingAudiencePolicy policy = policyRepository.findEffective(
                ownerType.name(),
                resource.getResourceType() == null ? null : resource.getResourceType().getCode(),
                resource.getResourceGroup() == null ? null : resource.getResourceGroup().getCode()
        ).orElse(null);
        if (policy == null) {
            return new EffectivePolicy(false);
        }

        int extraActive = 0;
        int extraDay = 0;
        int extraWeek = 0;
        int extraMonth = 0;
        List<BookingQuotaOverride> overrides = overrideRepository.findActive(ownerType.name(), ownerCode, resource.getCode());
        for (BookingQuotaOverride override : overrides) {
            extraActive += value(override.getExtraActiveBookings());
            extraDay += value(override.getExtraBookingsPerDay());
            extraWeek += value(override.getExtraBookingsPerWeek());
            extraMonth += value(override.getExtraBookingsPerMonth());
        }

        int minutes = Math.toIntExact(Duration.between(startedAt, endedAt).toMinutes());
        if (policy.getMaxBookingDurationMinutes() != null && minutes > policy.getMaxBookingDurationMinutes()) {
            throw new ConflictException("booking policy", "audience maximum booking duration exceeded");
        }
        if (policy.getMinBookingNoticeMinutes() != null && startedAt.isBefore(LocalDateTime.now().plusMinutes(policy.getMinBookingNoticeMinutes()))) {
            throw new ConflictException("booking policy", "audience minimum booking notice is not respected");
        }
        checkLimit(policy.getMaxActiveBookings(), extraActive, bookingRepository.countActiveForOwner(ownerType.name(), ownerCode), "active booking quota exceeded");

        LocalDate day = startedAt.toLocalDate();
        checkLimit(policy.getMaxBookingsPerDay(), extraDay, bookingRepository.countForOwnerBetween(ownerType.name(), ownerCode, day.atStartOfDay(), day.plusDays(1).atStartOfDay()), "daily booking quota exceeded");

        LocalDate weekStart = day.minusDays(day.getDayOfWeek().getValue() - 1L);
        checkLimit(policy.getMaxBookingsPerWeek(), extraWeek, bookingRepository.countForOwnerBetween(ownerType.name(), ownerCode, weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay()), "weekly booking quota exceeded");

        LocalDate monthStart = day.withDayOfMonth(1);
        checkLimit(policy.getMaxBookingsPerMonth(), extraMonth, bookingRepository.countForOwnerBetween(ownerType.name(), ownerCode, monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay()), "monthly booking quota exceeded");

        if (policy.getMaxNoShowsPerMonth() != null) {
            long noShows = bookingRepository.countNoShowsForOwnerBetween(ownerType.name(), ownerCode, monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay());
            if (noShows >= policy.getMaxNoShowsPerMonth()) {
                createNoShowTicket(ownerType, ownerCode, noShows);
                throw new ConflictException("booking policy", "monthly no-show quota exceeded");
            }
        }

        return new EffectivePolicy(Boolean.TRUE.equals(policy.getApprovalRequired()));
    }

    private void createNoShowTicket(SubscriberType ownerType, String ownerCode, long noShows) {
        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.executeWithoutResult(status -> supportTicketService.createFromAutomation(new CreateTicketRequest(
                    "Quota mensuel de no-show dépassé — " + ownerCode,
                    "Le client " + ownerCode + " (" + ownerType.name() + ") a atteint " + noShows
                            + " absence(s) non justifiée(s) ce mois-ci, dépassant le quota autorisé par sa politique de réservation.",
                    TicketPriority.HIGH,
                    TicketCategory.BOOKING,
                    ownerType.name(),
                    ownerCode,
                    null, null, null,
                    "BOOKING_NO_SHOW_QUOTA",
                    ownerCode
            )));
        } catch (Exception ex) {
            log.warn("Failed to create support ticket for no-show quota breach of owner {}/{}", ownerType, ownerCode, ex);
        }
    }

    private void checkLimit(Integer baseLimit, int extra, long current, String message) {
        if (baseLimit != null && current >= baseLimit + extra) {
            throw new ConflictException("booking quota", message);
        }
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    public record EffectivePolicy(boolean approvalRequired) {
    }
}
