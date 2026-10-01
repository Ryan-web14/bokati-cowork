package com.sni.bokaticowork.features.payment.compliance.repository;

import com.sni.bokaticowork.features.payment.compliance.model.ScreeningCheck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningCheckRepository extends JpaRepository<ScreeningCheck, Long> {

    Optional<ScreeningCheck> findByCheckNumber(String checkNumber);

    List<ScreeningCheck> findBySubjectTypeAndSubjectCodeOrderByCheckedAtDesc(String subjectType, String subjectCode);

    Optional<ScreeningCheck> findFirstBySubjectTypeAndSubjectCodeOrderByCheckedAtDesc(String subjectType, String subjectCode);

    Page<ScreeningCheck> findByResultInOrderByCheckedAtDesc(Collection<ScreeningCheck.Result> results, Pageable pageable);

    Page<ScreeningCheck> findAllByOrderByCheckedAtDesc(Pageable pageable);

    long countByResultInAndReviewedAtIsNull(Collection<ScreeningCheck.Result> results);
}
