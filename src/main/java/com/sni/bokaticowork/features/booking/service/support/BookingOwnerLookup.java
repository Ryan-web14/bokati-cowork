package com.sni.bokaticowork.features.booking.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.reference.lookup.LookupItem;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Recherche unique du titulaire d'une reservation (lot B7, ecart 13 de la doc) : membres, clients
 * et entreprises, par nom, code, courriel ou telephone, dans une seule liste deroulante.
 *
 * <p>Convention des recherches de choix du lot L0 ({@link LookupItem}) : {@code type} vaut
 * {@code MEMBER}, {@code CUSTOMER} ou {@code BUSINESS_ENTITY}, {@code code} est l'identifiant a
 * renvoyer dans {@code memberId}, {@code customerId} ou {@code businessCode}.</p>
 */
@Component
public class BookingOwnerLookup {

    public static final Set<String> TYPES = Set.of("MEMBER", "CUSTOMER", "BUSINESS_ENTITY");
    static final int MAX_SIZE = 50;

    private static final String MEMBER = """
            SELECT 'MEMBER' AS type, m.member_id AS code,
                   NULLIF(TRIM(COALESCE(m.firstname, '') || ' ' || COALESCE(m.lastname, '')), '') AS label,
                   COALESCE(m.email, m.phone) AS hint
            FROM member m
            WHERE m.deleted = false AND m.member_id IS NOT NULL
              AND (CAST(:like AS text) IS NULL OR m.member_id ILIKE :like OR COALESCE(m.firstname, '') ILIKE :like OR COALESCE(m.lastname, '') ILIKE :like
                   OR (COALESCE(m.firstname, '') || ' ' || COALESCE(m.lastname, '')) ILIKE :like
                   OR COALESCE(m.email, '') ILIKE :like OR COALESCE(m.phone, '') ILIKE :like)""";

    private static final String CUSTOMER = """
            SELECT 'CUSTOMER' AS type, c.customer_id AS code,
                   COALESCE(NULLIF(TRIM(c.company_name), ''),
                            NULLIF(TRIM(COALESCE(c.firstname, '') || ' ' || COALESCE(c.lastname, '')), '')) AS label,
                   COALESCE(c.email, c.phone) AS hint
            FROM customer c
            WHERE c.deleted = false
              AND (CAST(:like AS text) IS NULL OR c.customer_id ILIKE :like OR COALESCE(c.company_name, '') ILIKE :like
                   OR COALESCE(c.firstname, '') ILIKE :like OR COALESCE(c.lastname, '') ILIKE :like
                   OR (COALESCE(c.firstname, '') || ' ' || COALESCE(c.lastname, '')) ILIKE :like
                   OR COALESCE(c.email, '') ILIKE :like OR COALESCE(c.phone, '') ILIKE :like)""";

    private static final String BUSINESS = """
            SELECT 'BUSINESS_ENTITY' AS type, b.entity_code AS code, b.name AS label, COALESCE(b.email, b.phone) AS hint
            FROM business_entity b
            WHERE COALESCE(b.deleted, false) = false
              AND (CAST(:like AS text) IS NULL OR b.entity_code ILIKE :like OR b.name ILIKE :like
                   OR COALESCE(b.email, '') ILIKE :like OR COALESCE(b.phone, '') ILIKE :like)""";

    private final NamedParameterJdbcTemplate jdbc;

    public BookingOwnerLookup(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<LookupItem> search(String q, String types, int page, int size) {
        Set<String> wanted = types(types);
        PageRequest request = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_SIZE));
        String union = String.join("\nUNION ALL\n", branches(wanted));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("like", StringUtils.hasText(q) ? "%" + q.trim().replace("%", "\\%").replace("_", "\\_") + "%" : null)
                .addValue("limit", request.getPageSize())
                .addValue("offset", request.getOffset());
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + union + ") AS owners", params, Long.class);
        List<LookupItem> rows = jdbc.query("SELECT * FROM (" + union + ") AS owners ORDER BY label NULLS LAST, code LIMIT :limit OFFSET :offset",
                params, (rs, i) -> {
                    String code = rs.getString("code");
                    String label = rs.getString("label");
                    return new LookupItem(rs.getString("type"), code, StringUtils.hasText(label) ? label : code, rs.getString("hint"), true);
                });
        return new PaginatedResponse<>(new PageImpl<>(rows, request, total == null ? 0 : total));
    }

    static Set<String> types(String types) {
        if (!StringUtils.hasText(types)) {
            return new LinkedHashSet<>(List.of("MEMBER", "CUSTOMER", "BUSINESS_ENTITY"));
        }
        Set<String> wanted = new LinkedHashSet<>();
        for (String type : types.split(",")) {
            String normalized = type.trim().toUpperCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            if (!TYPES.contains(normalized)) {
                throw BadRequestException.coded("VALEUR_INCONNUE", "Type de titulaire inconnu « " + type.trim()
                        + " » ; valeurs permises : MEMBER, CUSTOMER, BUSINESS_ENTITY");
            }
            wanted.add(normalized);
        }
        return wanted.isEmpty() ? types(null) : wanted;
    }

    static List<String> branches(Set<String> wanted) {
        List<String> branches = new ArrayList<>();
        if (wanted.contains("MEMBER")) {
            branches.add(MEMBER);
        }
        if (wanted.contains("CUSTOMER")) {
            branches.add(CUSTOMER);
        }
        if (wanted.contains("BUSINESS_ENTITY")) {
            branches.add(BUSINESS);
        }
        return branches;
    }
}
