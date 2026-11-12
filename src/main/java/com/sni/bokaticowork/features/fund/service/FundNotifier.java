package com.sni.bokaticowork.features.fund.service;

import com.sni.bokaticowork.features.fund.model.FundRequest;
import com.sni.bokaticowork.features.fund.repository.FundApprovalDelegationRepository;
import com.sni.bokaticowork.features.payment.service.support.CashEmailNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Notifications des demandes de fonds (plan, section 6), par courriel avec lien vers l'ecran. */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundNotifier {

    static final String PATH = "/fund-requests/";

    private final CashEmailNotifier email;
    private final FundApprovalDelegationRepository delegations;
    private final DataSource dataSource;

    /** Valideurs d'un niveau : detenteurs du droit, et leurs delegues du jour. */
    void toApprovers(FundRequest request, int level) {
        String subject = "Demande de fonds a valider · " + request.getNumber();
        String message = request.getRequester() + " demande " + FundRequestService.plain(request.getTotalAmount()) + " XAF : « "
                + request.getSubject() + " ». Niveau " + level + " sur " + request.getRequiredLevels() + ".";
        recipients(FundRequestService.levelPermission(level).replace(':', '_'), level).stream()
                .filter(user -> !user.equalsIgnoreCase(request.getRequester()))
                .forEach(user -> email.notifyUser(user, subject, message, request.getNumber(), PATH + request.getNumber()));
    }

    void approved(FundRequest request) {
        toRequester(request, "Votre demande " + request.getNumber() + " est approuvee pour " + FundRequestService.plain(
                request.getApprovedAmount()) + " XAF. Presentez-vous a la caisse" + (request.getRegisterCode() == null ? "" : " " + request.getRegisterCode()) + ".");
        String subject = "Demande a decaisser · " + request.getNumber();
        recipients("FUND_REQUEST_DISBURSE", 0).forEach(user -> email.notifyUser(user, subject, "La demande " + request.getNumber()
                + " est approuvee et attend son decaissement.", request.getNumber(), PATH + request.getNumber()));
    }

    void disbursed(FundRequest request, BigDecimal amount) {
        String message = FundRequestService.plain(amount) + " XAF ont ete remis au titre de la demande " + request.getNumber()
                + (request.getJustificationDueDate() == null ? "." : ". Justifiez les depenses avant le " + request.getJustificationDueDate() + ".");
        toRequester(request, message);
        if (request.getBeneficiaryCode() != null && !request.getBeneficiaryCode().equalsIgnoreCase(request.getRequester())) {
            toBeneficiary(request, message);
        }
    }

    void toRequester(FundRequest request, String message) {
        email.notifyUser(request.getRequester(), "Demande de fonds " + request.getNumber(), message, request.getNumber(), PATH + request.getNumber());
    }

    void toBeneficiary(FundRequest request, String message) {
        email.notifyUser(request.getBeneficiaryCode(), "Avance " + request.getNumber(), message, request.getNumber(), PATH + request.getNumber());
    }

    void toSupervisor(FundRequest request, String message) {
        email.notifySupervisor("Avance en retard · " + request.getNumber(), message, request.getNumber(), PATH + request.getNumber());
    }

    /** Utilisateurs actifs qui detiennent la permission ({@code MODULE_ACTION}). */
    Set<String> recipients(String permission, int level) {
        Set<String> users = new LinkedHashSet<>();
        try {
            List<String> found = new JdbcTemplate(dataSource).queryForList(
                    "SELECT DISTINCT u.email FROM users u JOIN role_user ru ON ru.user_id = u.id JOIN role_permission rp ON rp.role_id = ru.role_id "
                            + "JOIN permission p ON p.id = rp.permission_id WHERE p.name = ? AND COALESCE(rp.is_active, TRUE) "
                            + "AND COALESCE(u.is_account_enabled, TRUE) AND u.email IS NOT NULL", String.class, permission);
            users.addAll(found);
        } catch (RuntimeException exception) {
            log.warn("Demandes de fonds · destinataires du droit {} introuvables : {}", permission, exception.getMessage());
        }
        if (level > 0) {
            LocalDate today = LocalDate.now(FundRequestService.ZONE);
            delegations.findByCancelledAtIsNullOrderByStartsOnAsc().stream()
                    .filter(delegation -> delegation.getLevel() == level && !delegation.getStartsOn().isAfter(today) && !delegation.getEndsOn().isBefore(today))
                    .forEach(delegation -> users.add(delegation.getDelegate()));
        }
        return users;
    }
}
