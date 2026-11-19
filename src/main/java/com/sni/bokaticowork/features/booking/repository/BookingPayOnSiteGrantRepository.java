package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingPayOnSiteGrant;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingPayOnSiteGrantRepository extends JpaRepository<BookingPayOnSiteGrant, Long> {

    Optional<BookingPayOnSiteGrant> findByOwnerTypeAndOwnerCode(SubscriberType ownerType, String ownerCode);

    List<BookingPayOnSiteGrant> findAllByOrderByUpdatedAtDesc();
}
