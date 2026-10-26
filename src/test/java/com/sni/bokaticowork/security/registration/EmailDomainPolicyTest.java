package com.sni.bokaticowork.security.registration;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'inscription refuse les adresses qui ne recevront jamais le code de verification.
 */
class EmailDomainPolicyTest {

    /** Un DNS de test · tout recoit du courrier, sauf ce qui est dit. */
    private static MailDomainResolver dns(Map<String, MailDomainResolver.Verdict> answers) {
        return domain -> answers.getOrDefault(domain, MailDomainResolver.Verdict.RECEIVES_MAIL);
    }

    private final EmailDomainPolicy policy = new EmailDomainPolicy(dns(Map.of(
            "domaine-sans-courrier.cg", MailDomainResolver.Verdict.NO_MAIL,
            "dns-en-panne.cg", MailDomainResolver.Verdict.UNKNOWN)), "concurrent-jetable.com", true);

    @ParameterizedTest
    @DisplayName("Les domaines reserves sont refuses · c'est celui du robot de l'incident")
    @ValueSource(strings = {"pentest-i60870@example.com", "a@example.org", "a@foo.test", "a@x.invalid",
            "a@machine.localhost", "a@sub.example.com", "A@EXAMPLE.COM"})
    void reservedDomainsAreRefused(String email) {
        assertThatThrownBy(() -> policy.assertAcceptable(email))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(EmailDomainPolicy.REFUSED_MESSAGE);
    }

    @ParameterizedTest
    @DisplayName("Les boites jetables sont refusees, sous-domaines et liste configuree compris")
    @ValueSource(strings = {"a@mailinator.com", "a@yopmail.com", "a@x.guerrillamail.com", "a@Concurrent-Jetable.com",
            "a@cool.fr.nf"})
    void disposableDomainsAreRefused(String email) {
        assertThatThrownBy(() -> policy.assertAcceptable(email)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Un domaine sans serveur de courrier est refuse")
    void domainWithoutMailIsRefused() {
        assertThat(policy.refusalReason("domaine-sans-courrier.cg")).isEqualTo("aucun serveur de courrier");
    }

    @Test
    @DisplayName("Une panne du DNS ne refuse personne")
    void dnsOutageDoesNotBlock() {
        assertThatCode(() -> policy.assertAcceptable("client@dns-en-panne.cg")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @DisplayName("Les adresses ordinaires passent")
    @ValueSource(strings = {"client@gmail.com", "direction@elleaose.com", "a.b@entreprise.cg", "x@outlook.fr"})
    void ordinaryAddressesPass(String email) {
        assertThatCode(() -> policy.assertAcceptable(email)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @DisplayName("Une adresse mal formee est refusee")
    @ValueSource(strings = {"", "sans-arobase", "a@", "@domaine.com", "a@sanspoint"})
    void malformedAddressesAreRefused(String email) {
        assertThatThrownBy(() -> policy.assertAcceptable(email)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("La verification MX peut etre coupee")
    void mxCheckCanBeDisabled() {
        EmailDomainPolicy withoutMx = new EmailDomainPolicy(dns(Map.of(
                "domaine-sans-courrier.cg", MailDomainResolver.Verdict.NO_MAIL)), "", false);

        assertThatCode(() -> withoutMx.assertAcceptable("a@domaine-sans-courrier.cg")).doesNotThrowAnyException();
    }
}
