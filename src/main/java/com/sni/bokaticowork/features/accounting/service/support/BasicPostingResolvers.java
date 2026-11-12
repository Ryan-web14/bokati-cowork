package com.sni.bokaticowork.features.accounting.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.accounting.repository.AccJournalRepository;
import com.sni.bokaticowork.features.accounting.repository.AccThirdPartyAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Locale;

/** Resolveurs de base du moteur d'imputation (lot F2). Les lots suivants ajoutent les leurs. */
@Configuration
public class BasicPostingResolvers {

    /** {@code {JOURNAL_ACCOUNT:code}} : compte de tresorerie d'un journal. */
    @Bean
    PostingResolver journalAccountResolver(AccJournalRepository journals) {
        return simple("JOURNAL_ACCOUNT", "Compte de tresorerie du journal donne (argument : code du journal)",
                (argument, event) -> StringUtils.hasText(argument)
                        ? journals.findByCode(argument.trim().toUpperCase(Locale.ROOT)).map(journal -> journal.getTreasuryAccountNumber()).orElse(null)
                        : null);
    }

    /** {@code {SETTING:cle}} : valeur d'un reglage de la plateforme (compte parametre). */
    @Bean
    PostingResolver settingResolver(PlatformSettings settings) {
        return simple("SETTING", "Valeur d'un reglage de la plateforme (argument : cle du reglage)",
                (argument, event) -> StringUtils.hasText(argument) ? settings.text(argument.trim(), null) : null);
    }

    /** {@code {THIRD_PARTY_ACCOUNT:code}} : compte collectif auquel un tiers est deja rattache. */
    @Bean
    PostingResolver thirdPartyAccountResolver(AccThirdPartyAccountRepository thirdParties) {
        return simple("THIRD_PARTY_ACCOUNT", "Compte collectif auquel le tiers est rattache (argument : code du tiers)",
                (argument, event) -> StringUtils.hasText(argument)
                        ? thirdParties.findFirstByPartyCodeOrderByIdAsc(argument.trim()).map(link -> link.getAccountNumber()).orElse(null)
                        : null);
    }

    public static PostingResolver simple(String name, String description, java.util.function.BiFunction<String, JsonNode, String> function) {
        return new PostingResolver() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public String resolve(String argument, JsonNode event) {
                return function.apply(argument, event);
            }
        };
    }
}
