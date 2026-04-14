package com.sni.bokaticowork.core.audit.repository;

import com.sni.bokaticowork.core.audit.model.SettingsAuditLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SettingsAuditLogsRepository extends JpaRepository<SettingsAuditLogs, Long>, JpaSpecificationExecutor<SettingsAuditLogs> {
}
