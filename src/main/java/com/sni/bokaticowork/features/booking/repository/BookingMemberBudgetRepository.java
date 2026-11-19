package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingMemberBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingMemberBudgetRepository extends JpaRepository<BookingMemberBudget, Long> {

    Optional<BookingMemberBudget> findByBusinessCodeAndMemberId(String businessCode, String memberId);

    List<BookingMemberBudget> findByBusinessCodeOrderByMemberIdAsc(String businessCode);
}
