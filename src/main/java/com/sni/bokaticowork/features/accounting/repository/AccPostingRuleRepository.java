package com.sni.bokaticowork.features.accounting.repository;

import com.sni.bokaticowork.features.accounting.enums.PostingRuleStatus;
import com.sni.bokaticowork.features.accounting.model.AccPostingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccPostingRuleRepository extends JpaRepository<AccPostingRule, Long> {

    List<AccPostingRule> findByEventTypeAndStatusOrderByPriorityAscVersionDesc(String eventType, PostingRuleStatus status);

    List<AccPostingRule> findByCodeOrderByVersionDesc(String code);

    Optional<AccPostingRule> findByCodeAndVersion(String code, Integer version);

    @Query("select r from AccPostingRule r where r.version = (select max(o.version) from AccPostingRule o where o.code = r.code) "
            + "and (:eventType is null or r.eventType = :eventType) order by r.family, r.eventType, r.priority, r.code")
    List<AccPostingRule> latestVersions(@Param("eventType") String eventType);
}
