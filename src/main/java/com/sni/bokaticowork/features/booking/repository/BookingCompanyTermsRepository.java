package com.sni.bokaticowork.features.booking.repository;

import com.sni.bokaticowork.features.booking.model.BookingCompanyTerms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingCompanyTermsRepository extends JpaRepository<BookingCompanyTerms, Long> {

    Optional<BookingCompanyTerms> findByBusinessCode(String businessCode);

    List<BookingCompanyTerms> findByMonthlyStatementTrue();
}
