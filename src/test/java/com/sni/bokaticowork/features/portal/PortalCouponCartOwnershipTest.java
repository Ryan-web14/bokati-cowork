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
class PortalCouponCartOwnershipTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponReservationRepository reservationRepository;
    @InjectMocks private CouponService service;

    private CouponReservation reservationOf(String memberId, String code) {
        Coupon coupon = new Coupon();
        coupon.setId(1L);
        coupon.setCode(code);
        CouponReservation reservation = new CouponReservation();
        reservation.setCoupon(coupon);
        reservation.setSubscriberType("MEMBER");
        reservation.setSubscriberCode(memberId);
        return reservation;
    }

    @Test
    @DisplayName("Retirer un code ne touche pas aux retenues d'un autre membre, ni aux autres codes")
    void releaseIsScopedToTheMemberAndTheCode() {
        CouponReservation mine = reservationOf("MBR-1", "BIENVENUE");
        CouponReservation otherCode = reservationOf("MBR-1", "ETE");
        CouponReservation someoneElse = reservationOf("MBR-2", "BIENVENUE");
        when(reservationRepository.findActiveByCart("PANIER-1")).thenReturn(List.of(mine, otherCode, someoneElse));

        int released = service.releaseFor("PANIER-1", "bienvenue", "MEMBER", "MBR-1", "retire");

        assertThat(released).isEqualTo(1);
    }

    @Test
    @DisplayName("Reprendre la reference du panier d'un autre ne reecrit pas sa retenue")
    void reserveOnAnotherMembersCartIsRefused() {
        CouponReservation someoneElse = reservationOf("MBR-2", "BIENVENUE");
        when(couponRepository.findByCodeForUpdate("BIENVENUE")).thenReturn(Optional.of(someoneElse.getCoupon()));
        when(reservationRepository.findActiveFor(1L, "PANIER-1")).thenReturn(Optional.of(someoneElse));

        assertThatThrownBy(() -> service.reserve("BIENVENUE", "PANIER-1", "MEMBER", "MBR-1",
                BigDecimal.TEN, "XAF", "MBR-1")).isInstanceOf(BadRequestException.class);
        verify(reservationRepository, never()).save(any());
    }
}
