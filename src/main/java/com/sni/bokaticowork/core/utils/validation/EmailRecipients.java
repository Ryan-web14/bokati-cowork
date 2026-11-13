package com.sni.bokaticowork.core.utils.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Destinataires d'un courriel dans un seul champ (lot B4) : {@code a@x, b@y|cc:c@z|bcc:d@w}. Le
 * champ voyage tel quel par la file d'envoi et le journal ; l'expediteur le decode au moment
 * d'adresser le message. Sans marqueur, c'est une simple liste de destinataires principaux.
 */
public record EmailRecipients(String to, List<String> cc, List<String> bcc) {

    private static final String CC = "|cc:";
    private static final String BCC = "|bcc:";

    public static String encode(List<String> to, List<String> cc, List<String> bcc) {
        StringBuilder value = new StringBuilder(String.join(", ", clean(to)));
        if (!clean(cc).isEmpty()) {
            value.append(CC).append(String.join(", ", clean(cc)));
        }
        if (!clean(bcc).isEmpty()) {
            value.append(BCC).append(String.join(", ", clean(bcc)));
        }
        return value.toString();
    }

    public static EmailRecipients parse(String value) {
        if (value == null || !value.contains("|")) {
            return new EmailRecipients(value, List.of(), List.of());
        }
        String to = value;
        List<String> cc = new ArrayList<>();
        List<String> bcc = new ArrayList<>();
        String[] parts = value.split("\\|");
        to = parts[0];
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            String lower = part.toLowerCase(Locale.ROOT);
            if (lower.startsWith("cc:")) {
                cc.addAll(EmailAddresses.split(part.substring(3)));
            } else if (lower.startsWith("bcc:")) {
                bcc.addAll(EmailAddresses.split(part.substring(4)));
            }
        }
        return new EmailRecipients(to, cc, bcc);
    }

    private static List<String> clean(List<String> addresses) {
        return addresses == null ? List.of() : addresses.stream().filter(address -> address != null && !address.isBlank())
                .map(String::trim).distinct().toList();
    }
}
