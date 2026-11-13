package com.sni.bokaticowork.features.approval.service;

import com.sni.bokaticowork.features.approval.dto.ApprovalDtos;
import com.sni.bokaticowork.security.admin.user.model.StaffProfile;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.StaffProfileRepository;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.authorization.SecurityRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hierarchie des collaborateurs (lot B15) : chaque utilisateur a son superieur direct (fiche
 * collaborateur, lot B12) ; la chaine remonte jusqu'a la direction (N+1, N+2, N+3...).
 */
@Component
@RequiredArgsConstructor
public class ApprovalHierarchy {

    private static final int MAX_DEPTH = 20;

    private final UserRepository users;
    private final StaffProfileRepository profiles;
    private final DataSource dataSource;

    /** Superieurs de l'utilisateur, du N+1 au sommet (identifiants de connexion). */
    public List<String> chain(String login) {
        List<String> chain = new ArrayList<>();
        Optional<Users> user = StringUtils.hasText(login) ? users.findByEmailIgnoreCase(login.trim()) : Optional.empty();
        if (user.isEmpty()) {
            return chain;
        }
        Set<Long> seen = new HashSet<>();
        Long cursor = user.get().getId();
        seen.add(cursor);
        while (chain.size() < MAX_DEPTH) {
            Long manager = profiles.findByUserId(cursor).map(StaffProfile::getManagerUserId).orElse(null);
            if (manager == null || !seen.add(manager)) {
                break;
            }
            Optional<Users> found = users.findById(manager);
            if (found.isEmpty()) {
                break;
            }
            chain.add(found.get().getEmail());
            cursor = manager;
        }
        return chain;
    }

    /** {@code actor} est-il au-dessus de {@code login} dans la hierarchie ? */
    public boolean isAbove(String actor, String login) {
        if (!StringUtils.hasText(actor) || !StringUtils.hasText(login)) {
            return false;
        }
        return chain(login).stream().anyMatch(above -> above.equalsIgnoreCase(actor.trim()));
    }

    /** Nom affiche d'un utilisateur ; l'identifiant s'il est inconnu. */
    public String nameOf(String login) {
        if (!StringUtils.hasText(login)) {
            return null;
        }
        return users.findByEmailIgnoreCase(login.trim()).map(ApprovalHierarchy::displayName).orElse(login.trim());
    }

    /** Organigramme : collaborateurs du back-office (fiche ou role d'administration) et leur superieur. */
    public List<ApprovalDtos.HierarchyNode> nodes() {
        String roles = SecurityRoles.ADMIN_REALM.stream().map(role -> "'" + role.replace("'", "").replaceFirst("^ROLE_", "") + "'").collect(Collectors.joining(","));
        return new JdbcTemplate(dataSource).query("""
                SELECT u.user_id, u.email, u.firstname, u.lastname, p.job_title, m.user_id AS manager_code,
                       m.firstname AS manager_first, m.lastname AS manager_last, m.email AS manager_email
                FROM users u
                LEFT JOIN user_staff_profile p ON p.user_id = u.id
                LEFT JOIN users m ON m.id = p.manager_user_id
                WHERE u.deleted = FALSE
                  AND (p.id IS NOT NULL OR EXISTS (SELECT 1 FROM role_user ru JOIN role r ON r.id = ru.role_id
                                                   WHERE ru.user_id = u.id AND r.name IN (%s)))
                ORDER BY lower(coalesce(u.lastname, u.email)), lower(coalesce(u.firstname, ''))
                """.formatted(roles.isEmpty() ? "''" : roles), (rs, i) -> new ApprovalDtos.HierarchyNode(
                rs.getString("user_id"), rs.getString("email"),
                name(rs.getString("firstname"), rs.getString("lastname"), rs.getString("email")),
                rs.getString("job_title"), rs.getString("manager_code"),
                rs.getString("manager_code") == null ? null
                        : name(rs.getString("manager_first"), rs.getString("manager_last"), rs.getString("manager_email"))));
    }

    static String displayName(Users user) {
        return name(user.getFirstname(), user.getLastname(), user.getEmail());
    }

    private static String name(String first, String last, String email) {
        String name = ((first == null ? "" : first.trim()) + " " + (last == null ? "" : last.trim())).trim();
        return name.isEmpty() ? email : name;
    }
}
