package com.sni.bokaticowork.features.portal;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.implementation.BookingServiceImpl;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.mapper.interfaces.NotificationMapper;
import com.sni.bokaticowork.features.notification.model.NotificationMessage;
import com.sni.bokaticowork.features.notification.repository.NotificationMessageRepository;
import com.sni.bokaticowork.features.notification.service.implementation.NotificationServiceImpl;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.Coupon;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.model.CouponReservation;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.repository.CouponReservationRepository;
import com.sni.bokaticowork.features.subscription.promotion.pricing.coupon.service.CouponService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Le portail ne rend ni ne modifie ce qui appartient a un autre membre · voir l'audit du lot 7. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalNotificationReadOwnershipTest {

    @Mock private NotificationMessageRepository messageRepository;
    @Mock private NotificationMapper mapper;
    @InjectMocks private NotificationServiceImpl service;

    @Test
    @DisplayName("La notification d'un autre n'est pas marquee lue, et repond introuvable")
    void anotherRecipientsNotificationIsNotTouched() {
        NotificationMessage message = new NotificationMessage();
        message.setRecipientEmail("autre@x.com");
        when(messageRepository.findByNotificationNumber("NTF-1")).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> service.markAsReadFor("NTF-1", "moi@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(messageRepository, never()).markOneAsRead(anyString(), any());
    }

    @Test
    @DisplayName("Sa propre notification est marquee lue")
    void ownNotificationIsMarked() {
        NotificationMessage message = new NotificationMessage();
        message.setRecipientEmail("moi@x.com");
        when(messageRepository.findByNotificationNumber("NTF-1")).thenReturn(Optional.of(message));
        when(mapper.toResponse(message)).thenReturn(org.mockito.Mockito.mock(NotificationMessageResponse.class));

        service.markAsReadFor("NTF-1", "Moi@X.com");

        verify(messageRepository).markOneAsRead(anyString(), any());
    }
}
