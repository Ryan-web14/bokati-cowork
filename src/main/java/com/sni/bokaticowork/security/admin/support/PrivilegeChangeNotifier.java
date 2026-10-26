package com.sni.bokaticowork.security.admin.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Previent tous les super-administrateurs de chaque changement de droits.
 *
 * <p>Un role attribue ou retire, les permissions d'un role modifiees, un compte d'administration
 * cree · autant d'actions qui, faites par un compte compromis, passeraient inapercues jusqu'a leur
 * premier usage. Chaque super-administrateur actif recoit le detail : qui, quoi, sur qui, quand.</p>
 *
 * <p>Envoye apres validation de la transaction · une modification annulee ne declenche pas
 * d'alerte. Un echec d'envoi est journalise et n'annule rien.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrivilegeChangeNotifier {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final RoleUserRepository roleUserRepository;
    private final DefaultEmailSender emailSender;

    /**
     * @param actor  l'adresse de l'acteur
     * @param action ce qui a ete fait, en une phrase
     * @param target sur qui ou sur quoi
     * @param detail le detail · roles, permissions
     */
    public void notify(String actor, String action, String target, String detail) {
        log.warn("Changement de droits · {} par {} sur {} · {}", action, actor, target, detail);
        List<String> recipients = roleUserRepository.findEnabledHolderEmails(PrivilegeGuard.SUPER_ADMIN);
        if (recipients.isEmpty()) {
            return;
        }
        String html = html(actor, action, target, detail, LocalDateTime.now());
        Runnable send = () -> recipients.forEach(recipient -> {
            try {
                emailSender.sendHtmlEmail(recipient, "Bokati · changement de droits : " + action, html);
            } catch (Exception ex) {
                log.warn("Alerte de changement de droits non envoyee a {} : {}", recipient, ex.getMessage());
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
            return;
        }
        send.run();
    }

    static String html(String actor, String action, String target, String detail, LocalDateTime when) {
        return """
                <!doctype html>
                <html>
                <body style="font-family:Arial,sans-serif;color:#1f2937;line-height:1.5;margin:0;padding:24px;background:#f8fafc;">
                  <div style="max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:8px;padding:24px;">
                    <h2 style="margin:0 0 16px;color:#111827;">Changement de droits</h2>
                    <table style="width:100%%;border-collapse:collapse;margin:18px 0;">
                      <tr><td style="padding:8px;border-bottom:1px solid #e5e7eb;color:#6b7280;">Action</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;"><strong>%s</strong></td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #e5e7eb;color:#6b7280;">Par</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;">%s</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #e5e7eb;color:#6b7280;">Sur</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;">%s</td></tr>
                      <tr><td style="padding:8px;border-bottom:1px solid #e5e7eb;color:#6b7280;">Detail</td><td style="padding:8px;border-bottom:1px solid #e5e7eb;">%s</td></tr>
                      <tr><td style="padding:8px;color:#6b7280;">Le</td><td style="padding:8px;">%s</td></tr>
                    </table>
                    <p style="color:#6b7280;font-size:13px;">Si vous ne reconnaissez pas ce changement, desactivez le compte qui l'a fait et revoquez le role concerne.</p>
                  </div>
                </body>
                </html>
                """.formatted(escape(action), escape(actor), escape(target), escape(detail), when.format(WHEN));
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
