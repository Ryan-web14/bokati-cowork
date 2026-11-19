package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.features.approval.enums.ApprovalStatus;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.service.ApprovalEngine;
import com.sni.bokaticowork.features.booking.dto.request.BookingApprovalRequest;
import com.sni.bokaticowork.features.booking.dto.request.BookingPriceAdjustmentRequest;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import com.sni.bokaticowork.features.booking.service.support.BookingExtras;
import com.sni.bokaticowork.features.booking.service.support.BookingPriceAdjuster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B4 : ajustement manuel du prix, plafond et approbation. */
class BookingB4Test {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final ApprovalEngine engine = mock(ApprovalEngine.class);
    private final BookingService bookingService = mock(BookingService.class);
    private final BookingExtras extras = mock(BookingExtras.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);

    @SuppressWarnings("unchecked")
    private BookingPriceAdjuster adjuster() {
        ObjectProvider<ApprovalEngine> approvals = mock(ObjectProvider.class);
        when(approvals.getIfAvailable()).thenReturn(engine);
        ObjectProvider<BookingService> service = mock(ObjectProvider.class);
        when(service.getObject()).thenReturn(bookingService);
        ObjectProvider<BookingExtras> extrasProvider = mock(ObjectProvider.class);
        when(extrasProvider.getObject()).thenReturn(extras);
        BookingPriceAdjuster adjuster = new BookingPriceAdjuster(bookings, approvals, service, extrasProvider, mock(BookingEventWriter.class));
        ReflectionTestUtils.setField(adjuster, "settings", settings);
        when(settings.integer(anyString(), anyInt())).thenAnswer(call -> call.getArgument(1));
        return adjuster;
    }

    private static void loginWith(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("agent@exemple.cg", "x",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    @AfterEach
    void logout() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void withinTheCapTheHolderOfTheRightAdjustsDirectly() {
        loginWith("ROLE_MANAGER", "BOOKING:PRICE_ADJUST");
        BookingPriceAdjuster.Plan plan = adjuster().plan(new BigDecimal("50000"), new BookingPriceAdjustmentRequest(new BigDecimal("10"), null, null, "Client fidèle"));
        assertThat(plan.after()).isEqualByComparingTo("45000");
        assertThat(plan.discount()).isEqualByComparingTo("5000");
        assertThat(plan.needsApproval()).isFalse();

        BookingPriceAdjuster.Plan beyond = adjuster().plan(new BigDecimal("50000"), new BookingPriceAdjustmentRequest(null, new BigDecimal("30000"), null, "Tarif négocié"));
        assertThat(beyond.rate()).isEqualByComparingTo("40");
        assertThat(beyond.needsApproval()).isTrue();
    }

    @Test
    void withoutTheRightEveryAdjustmentGoesToApprovalAndAnIncreaseIsRefused() {
        loginWith("ROLE_OPERATIONS_AGENT");
        BookingPriceAdjuster adjuster = adjuster();
        assertThat(adjuster.plan(new BigDecimal("50000"), new BookingPriceAdjustmentRequest(new BigDecimal("5"), null, null, "Geste")).needsApproval()).isTrue();
        assertThatThrownBy(() -> adjuster.plan(new BigDecimal("50000"), new BookingPriceAdjustmentRequest(null, new BigDecimal("60000"), null, "x")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("baisse le prix");
        assertThatThrownBy(() -> adjuster.plan(new BigDecimal("50000"), new BookingPriceAdjustmentRequest(new BigDecimal("5"), new BigDecimal("1"), null, "x")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> adjuster.plan(new BigDecimal("50000"), new BookingPriceAdjustmentRequest(new BigDecimal("5"), null, null, null)))
                .hasMessageContaining("motif");
    }

    @Test
    void beyondTheCapARequestIsSubmittedAndItsApprovalAppliesTheDiscountAndConfirms() {
        loginWith("ROLE_MANAGER", "BOOKING:PRICE_ADJUST");
        BookingPriceAdjuster adjuster = adjuster();
        Booking booking = Booking.builder().bookingNumber("BKG-9").status(BookingStatus.PENDING_APPROVAL).approvalForPriceOnly(true)
                .totalAmount(new BigDecimal("50000")).currency("XAF").build();
        when(bookings.findByBookingNumber("BKG-9")).thenReturn(Optional.of(booking));
        ApprovalRequest request = ApprovalRequest.builder().number("APR-1").status(ApprovalStatus.PENDING).build();
        when(engine.submit(any())).thenReturn(request);

        BookingPriceAdjuster.Plan plan = adjuster.plan(booking.getTotalAmount(), new BookingPriceAdjustmentRequest(new BigDecimal("40"), null, null, "Partenaire"));
        adjuster.requestApproval(booking, plan, "agent@exemple.cg");

        ArgumentCaptor<ApprovalDtos.Submission> submission = ArgumentCaptor.forClass(ApprovalDtos.Submission.class);
        verify(engine).submit(submission.capture());
        assertThat(submission.getValue().center()).isEqualTo("BOOKING");
        assertThat(submission.getValue().requestType()).isEqualTo("PRICE_ADJUSTMENT");
        assertThat(submission.getValue().subjectRef()).isEqualTo("BKG-9");
        assertThat(submission.getValue().amount()).isEqualByComparingTo("30000");
        assertThat(booking.getPriceAdjustmentStatus()).isEqualTo("PENDING");
        assertThat(booking.getPriceAdjustmentApprovalNumber()).isEqualTo("APR-1");
        verify(extras, never()).applyManualDiscount(any(), any(), anyString());

        adjuster.onDecision("BKG-9", true, "D'accord");
        assertThat(booking.getPriceAdjustmentStatus()).isEqualTo("APPLIED");
        verify(extras).applyManualDiscount(eq(booking), eq(new BigDecimal("20000.00")), anyString());
        verify(bookingService).approve(eq("BKG-9"), any(BookingApprovalRequest.class));
    }

    @Test
    void aRefusalKeepsTheComputedPriceAndLetsTheBookingGoOn() {
        BookingPriceAdjuster adjuster = adjuster();
        Booking booking = Booking.builder().bookingNumber("BKG-8").status(BookingStatus.PENDING_APPROVAL).approvalForPriceOnly(true)
                .priceAdjustmentStatus("PENDING").originalAmount(new BigDecimal("50000")).priceAdjustmentAmount(new BigDecimal("20000"))
                .totalAmount(new BigDecimal("50000")).build();
        when(bookings.findByBookingNumber("BKG-8")).thenReturn(Optional.of(booking));

        adjuster.onDecision("BKG-8", false, "Trop élevé");

        assertThat(booking.getPriceAdjustmentStatus()).isEqualTo("REJECTED");
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("50000");
        verify(extras, never()).applyManualDiscount(any(), any(), anyString());
        verify(bookingService).approve(eq("BKG-8"), any(BookingApprovalRequest.class));
        assertThat(List.of(booking.getPriceAdjustmentStatus())).containsExactly("REJECTED");
    }
}
