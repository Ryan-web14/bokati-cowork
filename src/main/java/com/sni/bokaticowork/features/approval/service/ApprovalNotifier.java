package com.sni.bokaticowork.features.approval.service;

import com.sni.bokaticowork.features.approval.model.ApprovalDelegation;
import com.sni.bokaticowork.features.approval.model.ApprovalRequest;
import com.sni.bokaticowork.features.approval.model.ApprovalStep;
import com.sni.bokaticowork.features.approval.repository.ApprovalDelegationRepository;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Notifications des centres d'approbation : dans l'application et par courriel (lot B15). */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalNotifier {

    private final CashEmailNotifier email;
    private final AdminInAppNotifier inApp;
    private final ApprovalDelegationRepository delegations;
    private final DataSource dataSource;

    /** Ceux qui peuvent decider du niveau : decideur attendu, detenteurs du droit, delegues du jour. */
    public void toApprovers(ApprovalRequest request, ApprovalStep step, String path, boolean reminder) {
        String subject = (reminder ? "Rappel · " : "") + "Demande à approuver · " + request.getNumber();
        String message = (request.getRequesterName() != null ? request.getRequesterName() : request.getRequester()) + " demande votre accord : « "
                + request.getTitle() + " »" + amount(request) + ". Niveau " + step.getLevelOrder() + " sur " + request.getRequiredLevels()
                + " (" + step.getLabel() + ").";
        recipients(request, step).forEach(user -> send(user, "APPROVAL_PENDING", subject, message, request, path));
    }

    public void toRequester(ApprovalRequest request, String message, String path) {
        send(request.getRequester(), "APPROVAL_DECIDED", "Demande " + request.getNumber(), message, request, path);
    }

    /** Escalade : le superieur du decideur attendu, a defaut le responsable de permanence. */
    public void escalate(ApprovalRequest request, ApprovalStep step, String above, long hours, String path) {
        String subject = "Demande en attente · " + request.getNumber();
        String message = "La demande « " + request.getTitle() + " » attend depuis " + hours + " heures la décision du niveau "
                + step.getLevelOrder() + " (" + step.getLabel() + (step.getExpectedApproverName() != null ? ", " + step.getExpectedApproverName() : "")
                + "). Vous pouvez l'approuver en avance.";
        if (StringUtils.hasText(above)) {
            send(above, "APPROVAL_ESCALATED", subject, message, request, path);
        } else {
            email.notifySupervisor(subject, message, request.getNumber(), path);
        }
    }

    Set<String> recipients(ApprovalRequest request, ApprovalStep step) {
        Set<String> users = new LinkedHashSet<>();
        if (StringUtils.hasText(step.getExpectedApprover())) {
            users.add(step.getExpectedApprover());
        }
        if (StringUtils.hasText(step.getPermission())) {
            try {
                users.addAll(new JdbcTemplate(dataSource).queryForList(
                        "SELECT DISTINCT u.email FROM users u JOIN role_user ru ON ru.user_id = u.id JOIN role_permission rp ON rp.role_id = ru.role_id "
                                + "JOIN permission p ON p.id = rp.permission_id WHERE p.name = ? AND COALESCE(rp.is_active, TRUE) "
                                + "AND COALESCE(u.is_account_enabled, TRUE) AND u.deleted = FALSE AND u.email IS NOT NULL",
                        String.class, step.getPermission().replace(':', '_')));
            } catch (RuntimeException exception) {
                log.warn("Approbations · detenteurs du droit {} introuvables : {}", step.getPermission(), exception.getMessage());
            }
        }
        LocalDate today = LocalDate.now(ApprovalEngine.ZONE);
        for (ApprovalDelegation delegation : delegations.findByCancelledAtIsNullOrderByStartsOnAsc()) {
            if (!delegation.activeOn(today) || (delegation.getCenter() != null && !delegation.getCenter().equalsIgnoreCase(request.getCenter()))) {
                continue;
            }
            boolean byLevel = delegation.getLevelOrder() != null && delegation.getLevelOrder().equals(step.getLevelOrder());
            boolean byPerson = delegation.getLevelOrder() == null && StringUtils.hasText(step.getExpectedApprover())
                    && delegation.getDelegator().equalsIgnoreCase(step.getExpectedApprover());
            if (byLevel || byPerson) {
                users.add(delegation.getDelegate());
            }
        }
        users.removeIf(user -> user.equalsIgnoreCase(request.getRequester()));
        return users;
    }

    private void send(String user, String eventType, String subject, String message, ApprovalRequest request, String path) {
        try {
            email.notifyUser(user, subject, message, request.getNumber(), path);
        } catch (RuntimeException exception) {
            log.warn("Approbations · courriel a {} non envoye : {}", user, exception.getMessage());
        }
        inApp.notify(eventType, "APPROVAL_REQUEST", request.getNumber(), user, subject,
                Map.of("number", request.getNumber(), "center", request.getCenter(), "requestType", request.getRequestType(),
                        "subjectRef", request.getSubjectRef(), "message", message, "path", path == null ? "" : path));
    }

    private static String amount(ApprovalRequest request) {
        BigDecimal amount = request.getAmount();
        return amount == null ? "" : " pour " + amount.stripTrailingZeros().toPlainString() + " " + (request.getCurrency() == null ? "XAF" : request.getCurrency());
    }
}
