package com.sni.bokaticowork.features.support;

import com.sni.bokaticowork.features.support.worker.SupportInboundEmailWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les courriels que la plateforme s'envoie (nouveau message, delai depasse) ne deviennent pas des
 * messages du client : ils viennent de la boite du support ou de l'adresse du responsable.
 */
class SupportInboundOwnMailboxTest {

    @Test
    @DisplayName("La boite du support et l'adresse du responsable sont reconnues, sans tenir compte de la casse")
    void ownMailboxIsRecognised() {
        SupportInboundEmailWorker worker = new SupportInboundEmailWorker(null, null, null, null, null);
        ReflectionTestUtils.setField(worker, "supportInbox", "supportela@elleaose.com");
        ReflectionTestUtils.setField(worker, "managerEmail", "direction@elleaose.com");

        assertThat(worker.isOwnMailbox("SupportELA@elleaose.com")).isTrue();
        assertThat(worker.isOwnMailbox(" direction@elleaose.com ")).isTrue();
        assertThat(worker.isOwnMailbox("admin@sni-cg.com")).isFalse();
        assertThat(worker.isOwnMailbox("")).isFalse();
    }

    @Test
    @DisplayName("Seules les notifications de la plateforme sont ecartees : une reponse humaine, meme depuis la boite du support, passe")
    void onlyPlatformNotificationsAreSkipped() {
        SupportInboundEmailWorker worker = new SupportInboundEmailWorker(null, null, null, null, null);
        ReflectionTestUtils.setField(worker, "supportInbox", "supportela@elleaose.com");
        ReflectionTestUtils.setField(worker, "managerEmail", "supportela@elleaose.com");
        String notification = "Elle A Osé Service client & support Bonjour, Délai SLA dépassé : RESOLUTION Ticket TCK-1 "
                + "Ticket de référence : TCK-1 Voir mon ticket Vous pouvez répondre directement à cet email : "
                + "votre message sera ajouté à votre ticket (gardez la référence TCK-1 dans l'objet).";

        assertThat(worker.isPlatformNotification("supportela@elleaose.com", notification)).isTrue();
        // Un agent qui repond depuis la boite du support
        assertThat(worker.isPlatformNotification("supportela@elleaose.com", "Bonjour, le technicien passe demain.")).isFalse();
        // Un client dont la reponse garde la notification citee
        assertThat(worker.isPlatformNotification("client@exemple.cg", "Merci ! " + notification)).isFalse();
    }
}
