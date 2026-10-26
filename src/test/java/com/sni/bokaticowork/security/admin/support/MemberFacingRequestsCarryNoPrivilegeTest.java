package com.sni.bokaticowork.security.admin.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aucune requete qu'un membre ou un inconnu peut envoyer ne porte de champ de droits.
 *
 * <p>Un champ {@code roles} ou {@code isAccountEnabled} dans un DTO du portail ou de
 * l'inscription, recopie par un mapper, suffirait a ce qu'un membre s'attribue un role en
 * l'ajoutant a sa requete. Aucun n'en porte aujourd'hui · ce test empeche qu'un ajout futur
 * passe inapercu.</p>
 */
class MemberFacingRequestsCarryNoPrivilegeTest {

    private static final List<String> PACKAGES = List.of(
            "com.sni.bokaticowork.features.portal",
            "com.sni.bokaticowork.security.authentication.dto.request");

    private static final Set<String> FORBIDDEN = Set.of(
            "role", "roles", "rolenames", "rolename", "permissions", "authorities",
            "isaccountenabled", "isaccountlocked", "isaccountexpired", "accountenabled", "enabled", "locked",
            "portalaccess", "status", "emailverifiedat", "emailverifiedby", "admin", "superadmin");

    @Test
    @DisplayName("Les DTO de requete du portail et de l'inscription n'ont aucun champ de droits")
    void noPrivilegeFieldInMemberFacingRequests() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*\\.dto\\.request\\..*")));

        List<String> offenders = new ArrayList<>();
        int scanned = 0;
        for (String basePackage : PACKAGES) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                Class<?> type = Class.forName(candidate.getBeanClassName());
                scanned++;
                for (Field field : type.getDeclaredFields()) {
                    if (FORBIDDEN.contains(field.getName().toLowerCase(Locale.ROOT))) {
                        offenders.add(type.getSimpleName() + "." + field.getName());
                    }
                }
            }
        }

        assertThat(scanned).as("le scan trouve bien les DTO").isGreaterThan(5);
        assertThat(offenders).isEmpty();
    }
}
