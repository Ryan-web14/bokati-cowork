package com.sni.bokaticowork.core.communication.mailService.repository;

import com.sni.bokaticowork.core.communication.mailService.model.EmailDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface EmailDeliveryLogRepository extends JpaRepository<EmailDeliveryLog, Long>, JpaSpecificationExecutor<EmailDeliveryLog> {

    Optional<EmailDeliveryLog> findByEmailNumber(String emailNumber);

    Optional<EmailDeliveryLog> findFirstByDedupKeyInOrderByCreatedAtDesc(Collection<String> dedupKeys);
}
