package com.sni.bokaticowork.core.audit.service.implementation;


import com.sni.bokaticowork.core.audit.dto.request.AuditLogFilter;
import com.sni.bokaticowork.core.audit.model.AuditLog;
import com.sni.bokaticowork.core.audit.repository.AuditLogRepository;
import com.sni.bokaticowork.core.audit.service.interfaces.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AuditServiceImpl implements AuditService {

    private static final int EXPORT_PAGE = 1_000;

    private final AuditLogRepository auditRepo;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLog auditLog) {
        auditRepo.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> list(AuditLogFilter filter, Pageable pageable) {
        return auditRepo.findAll(specification(filter), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditExport export(AuditLogFilter filter, int maxRows) {
        StringBuilder csv = new StringBuilder("﻿");
        csv.append(String.join(";", "date", "acteur", "module", "action", "ressource", "type_element", "code_element",
                "resultat", "adresse_ip", "identifiant_requete", "erreur", "changements")).append("\r\n");
        Specification<AuditLog> specification = specification(filter);
        int rows = 0;
        int page = 0;
        boolean truncated = false;
        while (true) {
            Page<AuditLog> slice = auditRepo.findAll(specification,
                    PageRequest.of(page++, EXPORT_PAGE, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
            for (AuditLog log : slice.getContent()) {
                if (rows >= maxRows) {
                    truncated = true;
                    break;
                }
                csv.append(String.join(";",
                        cell(log.getCreatedAt()), cell(log.getActorEmail()), cell(log.getModule()), cell(log.getAction()),
                        cell(log.getRessource()), cell(log.getEntityType()), cell(log.getEntityCode()), cell(log.getAuditStatus()),
                        cell(log.getIpAddress()), cell(log.getRequestId()), cell(log.getErrorCode()), cell(changes(log.getDiff()))))
                        .append("\r\n");
                rows++;
            }
            if (truncated || !slice.hasNext()) {
                break;
            }
        }
        return new AuditExport(csv.toString(), rows, truncated);
    }

    /** Changements en une ligne : {@code Type code : champ avant -> apres, ...}. */
    @SuppressWarnings("unchecked")
    static String changes(Map<String, Object> diff) {
        if (diff == null || !(diff.get("changes") instanceof List<?> list)) {
            return diff == null ? null : String.valueOf(diff);
        }
        StringBuilder text = new StringBuilder();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> change)) {
                continue;
            }
            if (text.length() > 0) {
                text.append(" | ");
            }
            text.append(change.get("operation")).append(' ').append(change.get("entityType")).append(' ').append(change.get("entityCode"));
            if (change.get("fields") instanceof Map<?, ?> fields && !fields.isEmpty()) {
                text.append(" :");
                fields.forEach((name, value) -> {
                    Map<String, Object> pair = value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
                    text.append(' ').append(name).append(' ').append(pair.get("before")).append(" -> ").append(pair.get("after")).append(',');
                });
                text.setLength(text.length() - 1);
            }
        }
        return text.toString();
    }

    private static String cell(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replace("\r", " ").replace("\n", " ");
        // Formules de tableur neutralisees.
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.contains(";") || text.contains("\"")) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private Specification<AuditLog> specification(AuditLogFilter filter) {
        Specification<AuditLog> specification = (root, query, cb) -> cb.conjunction();
        if (filter == null) {
            return specification;
        }
        if (StringUtils.hasText(filter.module())) {
            String module = upper(filter.module());
            specification = specification.and((root, query, cb) -> cb.equal(cb.upper(root.get("module")), module));
        }
        if (StringUtils.hasText(filter.action())) {
            String action = upper(filter.action());
            specification = specification.and((root, query, cb) -> cb.equal(cb.upper(root.get("action")), action));
        }
        if (StringUtils.hasText(filter.actorEmail())) {
            String pattern = "%" + filter.actorEmail().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("actorEmail")), pattern));
        }
        if (filter.status() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("auditStatus"), filter.status()));
        }
        if (filter.createdFrom() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), filter.createdFrom()));
        }
        if (filter.createdTo() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), filter.createdTo()));
        }
        if (StringUtils.hasText(filter.entityType())) {
            String type = filter.entityType().trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, cb) -> cb.equal(cb.lower(root.get("entityType")), type));
        }
        if (StringUtils.hasText(filter.entityCode())) {
            String code = upper(filter.entityCode());
            specification = specification.and((root, query, cb) -> cb.equal(cb.upper(root.get("entityCode")), code));
        }
        if (StringUtils.hasText(filter.requestId())) {
            String requestId = filter.requestId().trim();
            specification = specification.and((root, query, cb) -> cb.equal(root.get("requestId"), requestId));
        }
        if (StringUtils.hasText(filter.ressource())) {
            String pattern = "%" + filter.ressource().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("ressource")), pattern));
        }
        return specification;
    }

    private static String upper(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
