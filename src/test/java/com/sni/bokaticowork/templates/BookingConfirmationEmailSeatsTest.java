package com.sni.bokaticowork.templates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'e-mail de confirmation dit ce qui est reserve · 2 places d'un bureau de 4 ne s'affichent plus
 * « Capacité : 4 personne(s) ».
 */
class BookingConfirmationEmailSeatsTest {

    private final SpringTemplateEngine engine = engine();

    @Test
    @DisplayName("Espace partage : la ligne Places porte les places reservees")
    void sharedSpaceShowsReservedSeats() {
        String html = render(Map.of("seatsLabel", "2 places réservées sur 4", "wholeResource", false,
                "resourceName", "Bureau 4 places", "recipientName", "Joel"));

        assertThat(html).contains("2 places réservées sur 4").contains(">Places<");
        assertThat(html).doesNotContain("Capacité").doesNotContain("personne(s)");
    }

    @Test
    @DisplayName("Salle louee entiere : la ligne s'intitule Réservation")
    void wholeRoomShowsParticipants() {
        String html = render(Map.of("seatsLabel", "Espace entier · 3 participants (12 places)", "wholeResource", true,
                "resourceName", "Salle Jade", "recipientName", "Joel"));

        assertThat(html).contains("Espace entier · 3 participants (12 places)").contains(">Réservation<");
    }

    @Test
    @DisplayName("Paiement attendu : le titre ne dit pas « confirmée »")
    void pendingPaymentIsNotAnnouncedAsConfirmed() {
        String html = render(Map.of("awaitingPayment", true, "headline", "Réservation enregistrée",
                "statusLabel", "Paiement attendu", "paymentStatusLabel", "En attente de paiement",
                "resourceName", "Open space", "recipientName", "Joel", "seatsLabel", "2 places réservées sur 4",
                "seatsCount", 2));

        assertThat(html).contains("Réservation enregistrée").contains("Paiement attendu")
                .contains("dès réception du paiement").doesNotContain(">Réservation confirmée<");
    }

    @Test
    @DisplayName("Couverte par un abonnement : pas de detail de prix")
    void coveredBookingHidesPrice() {
        String html = render(Map.of("covered", true, "resourceName", "Open space", "recipientName", "Joel",
                "priceTTC", "10 000 XAF"));

        assertThat(html).contains("couverte par votre abonnement").doesNotContain("Total TTC");
    }

    private String render(Map<String, Object> model) {
        Context context = new Context(Locale.FRANCE);
        model.forEach(context::setVariable);
        return engine.process("email/booking-confirmation", context);
    }

    private SpringTemplateEngine engine() {
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        // Les en-tetes citent #branding.logoUrl() · sans le dialecte, le gabarit ne s analyse pas.
        engine.addDialect(new com.sni.bokaticowork.core.communication.mailService.config.BrandingDialect(
                new com.sni.bokaticowork.core.communication.mailService.support.EmailBranding("https://cowork.elleaose.cg")));
        return engine;
    }
}
