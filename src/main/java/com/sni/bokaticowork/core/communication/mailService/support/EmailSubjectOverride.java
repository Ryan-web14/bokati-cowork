package com.sni.bokaticowork.core.communication.mailService.support;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Objet d'un courriel produit par un modele de documents (lot E1) : le gabarit genere le porte dans
 * un element cache {@code x-email-subject}. A l'envoi, cet objet remplace celui du code s'il n'est
 * pas vide, et l'element est retire du corps. Un courriel sans cet element passe tel quel.
 */
public final class EmailSubjectOverride {

    public static final String MARKER = "id=\"x-email-subject\"";

    public record Mail(String subject, String content) {
    }

    private EmailSubjectOverride() {
    }

    public static Mail apply(String subject, String content) {
        if (content == null || !content.contains(MARKER)) {
            return new Mail(subject, content);
        }
        Document document = Jsoup.parse(content);
        Element element = document.getElementById("x-email-subject");
        if (element == null) {
            return new Mail(subject, content);
        }
        String override = element.text().trim();
        element.remove();
        return new Mail(override.isEmpty() ? subject : override, document.outerHtml());
    }
}
