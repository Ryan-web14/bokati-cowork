package com.sni.bokaticowork.core.maintenance;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataPurgeService {

    private final JdbcTemplate jdbcTemplate;

    private static final Set<String> TABLES_NEVER_TOUCHED = Set.of(
            // Flyway
            "flyway_schema_history",

            // RBAC (seeded V8, V142, V148, V149, V150)
            "role",
            "permission",
            "role_permission",

            // Sequence engine (seeded V1, V12, V122, V123, V124)
            "sequence_definition",
            "sequence_counter",

            // Reference data (seeded V11)
            "country",
            "currency",

            // Document catalog (seeded V23, V24)
            "document_type",
            "document_requirement",
            "document_sequence",

            // Contract templates (seeded V163)
            "contract_template",
            "contract_template_clause",
            "contract_template_policy",

            // Billing reference (seeded in migrations)
            "billing_tax_rule",
            "billing_clause_template",
            "tax_rule",

            // Resource reference (seeded in migrations)
            "resource_type",
            "resource_group",
            "resource_policy",

            // Booking reference (seeded V128)
            "cancellation_policy",

            // Inventory reference (seeded in migrations)
            "inventory_category",
            "inventory_location",
            "inventory_unit",
            "inventory_unit_conversion",

            // Purchase reference (seeded in migrations)
            "purchase_approval_rule",

            // Notification templates (seeded in migrations)
            "notification_template"
    );

    private static final Set<String> TABLES_SELECTIVE_DELETE = Set.of(
            "users",
            "role_user"
    );

    @Transactional
    public DataPurgeResult purgeAllNonAdminData() {
        String env = resolveEnvironment();
        if ("prod".equalsIgnoreCase(env) || "production".equalsIgnoreCase(env)) {
            throw new BadRequestException("Data purge is not allowed in production environment");
        }

        log.warn("=== DATA PURGE INITIATED === Environment: {}", env);
        Instant start = Instant.now();

        List<Long> adminUserIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT u.id FROM users u " +
                        "JOIN role_user ru ON u.id = ru.user_id " +
                        "JOIN role r ON ru.role_id = r.id " +
                        "WHERE r.name IN ('ADMIN', 'SUPER_ADMIN')",
                Long.class
        );

        log.info("Preserving {} admin user(s): {}", adminUserIds.size(), adminUserIds);

        if (adminUserIds.isEmpty()) {
            throw new BadRequestException("No admin users found — aborting purge to prevent total data loss");
        }

        // Step 1: TRUNCATE all tables except system tables and users/role_user
        List<String> tablesToTruncate = discoverTruncatableTables();
        log.info("Found {} tables to truncate", tablesToTruncate.size());

        if (!tablesToTruncate.isEmpty()) {
            String truncateList = tablesToTruncate.stream()
                    .map(t -> "\"" + t + "\"")
                    .collect(Collectors.joining(", "));
            jdbcTemplate.execute("TRUNCATE TABLE " + truncateList + " CASCADE");
            log.info("Truncated {} tables with CASCADE", tablesToTruncate.size());
        }

        // Step 2: Delete non-admin users and their role assignments
        String adminIdList = adminUserIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        int nonAdminUsers = purgeNonAdminUsers(adminIdList);
        int nonAdminRoles = purgeNonAdminRoleAssignments(adminIdList);

        // Step 3: Reset sequences
        resetSequenceCounters();

        long durationMs = java.time.Duration.between(start, Instant.now()).toMillis();
        log.warn("=== DATA PURGE COMPLETE === {} table(s) truncated, {} user(s) deleted, {} role assignment(s) deleted. {} admin(s) preserved. Duration: {} ms",
                tablesToTruncate.size(), nonAdminUsers, nonAdminRoles, adminUserIds.size(), durationMs);

        return new DataPurgeResult(tablesToTruncate.size(), nonAdminUsers, nonAdminRoles, adminUserIds.size(), durationMs);
    }

    private List<String> discoverTruncatableTables() {
        List<String> allTables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                        "ORDER BY table_name",
                String.class
        );
        return allTables.stream()
                .filter(t -> !TABLES_NEVER_TOUCHED.contains(t))
                .filter(t -> !TABLES_SELECTIVE_DELETE.contains(t))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private int purgeNonAdminUsers(String adminIdList) {
        // role_user has FK to users — delete role assignments first, then users
        // But role_user is handled separately, so just delete users here.
        // FK from member/customer to users was already broken by TRUNCATE CASCADE on member/customer.
        int deleted = jdbcTemplate.update("DELETE FROM users WHERE id NOT IN (" + adminIdList + ")");
        log.info("  Purged {} non-admin user(s)", deleted);
        return deleted;
    }

    private int purgeNonAdminRoleAssignments(String adminIdList) {
        int deleted = jdbcTemplate.update("DELETE FROM role_user WHERE user_id NOT IN (" + adminIdList + ")");
        log.info("  Purged {} non-admin role assignment(s)", deleted);
        return deleted;
    }

    private void resetSequenceCounters() {
        try {
            int reset = jdbcTemplate.update("UPDATE sequence_counter SET current_value = 0");
            if (reset > 0) {
                log.info("  Reset {} sequence counter(s)", reset);
            }
        } catch (Exception ex) {
            log.debug("  sequence_counter reset skipped: {}", ex.getMessage());
        }
    }

    private String resolveEnvironment() {
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        if (env != null && !env.isBlank()) return env.split(",")[0].trim();
        return System.getProperty("spring.profiles.active", "dev");
    }

    public record DataPurgeResult(int tablesTruncated, int nonAdminUsersDeleted, int nonAdminRolesDeleted,
                                   int adminUsersPreserved, long durationMs) {}
}
