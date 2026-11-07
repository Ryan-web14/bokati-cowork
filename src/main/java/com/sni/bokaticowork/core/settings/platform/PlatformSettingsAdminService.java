package com.sni.bokaticowork.core.settings.platform;

import com.sni.bokaticowork.core.audit.model.SettingsAuditLogs;
import com.sni.bokaticowork.core.audit.repository.SettingsAuditLogsRepository;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consulter et modifier les parametres · {@code SUPER_ADMIN} tous, {@code ADMIN} les domaines qui lui
 * sont ouverts. Chaque changement laisse son ancienne valeur, la nouvelle et son auteur au journal.
 */
@Service
@RequiredArgsConstructor
public class PlatformSettingsAdminService {

    private static final String SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ADMIN = "ROLE_ADMIN";
    private static final int HISTORY_SIZE = 100;

    private final PlatformSettingRepository repository;
    private final SettingsAuditLogsRepository auditRepository;
    private final PlatformSettings settings;
    private final ConfigurableEnvironment environment;
    private final Clock clock = Clock.systemUTC();

    /**
     * @param value          la valeur en vigueur · toujours vide pour un secret
     * @param defaultValue   celle du fichier de configuration · toujours vide pour un secret
     * @param modified       une valeur a ete enregistree depuis l'administration
     * @param configured     un secret est renseigne sur le serveur · jamais sa valeur
     * @param restartPending enregistree, elle attend le prochain redemarrage
     */
    public record SettingView(String key, String label, SettingDomain domain, PlatformSettingCatalog.Type type,
                              PlatformSettingCatalog.Effect effect, String value, String defaultValue, boolean modified,
                              boolean editable, Boolean configured, boolean restartPending, String updatedBy, Instant updatedAt) {
    }

    public record DomainView(SettingDomain domain, String label, boolean adminEditable, int settings) {
    }

    public record ChangeView(String oldValue, String newValue, String reason, Instant changedAt) {
    }

    @Transactional(readOnly = true)
    public List<DomainView> domains(Authentication authentication) {
        return Arrays.stream(SettingDomain.values())
                .filter(domain -> mayAccess(domain, authentication))
                .map(domain -> new DomainView(domain, domain.label(), domain.adminEditable(),
                        (int) PlatformSettingCatalog.all().stream().filter(definition -> definition.domain() == domain).count()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SettingView> list(SettingDomain domain, Authentication authentication) {
        Map<String, PlatformSetting> stored = repository.findAll().stream()
                .collect(Collectors.toMap(PlatformSetting::getKey, Function.identity()));
        return PlatformSettingCatalog.all().stream()
                .filter(definition -> domain == null || definition.domain() == domain)
                .filter(definition -> mayAccess(definition.domain(), authentication))
                .map(definition -> view(definition, stored.get(definition.key())))
                .toList();
    }

    @Transactional
    public SettingView update(String key, String rawValue, String reason, Authentication authentication) {
        PlatformSettingCatalog.Definition definition = editable(key, authentication);
        String value = PlatformSettingValidator.validated(definition, rawValue);
        PlatformSetting stored = repository.findById(key).orElse(null);
        String previous = stored != null ? stored.getValue() : defaultValue(definition);
        if (stored == null) {
            stored = PlatformSetting.builder().key(key).build();
        }
        stored.setValue(value);
        stored.setUpdatedBy(authentication.getName());
        stored.setUpdatedAt(clock.instant());
        repository.save(stored);
        audit(key, previous, value, reason, authentication);
        refreshAfterCommit();
        return view(definition, stored);
    }

    /** Rend le reglage au fichier de configuration. */
    @Transactional
    public SettingView reset(String key, String reason, Authentication authentication) {
        PlatformSettingCatalog.Definition definition = editable(key, authentication);
        repository.findById(key).ifPresent(stored -> {
            audit(key, stored.getValue(), defaultValue(definition), reason, authentication);
            repository.delete(stored);
        });
        refreshAfterCommit();
        return view(definition, null);
    }

    @Transactional(readOnly = true)
    public List<ChangeView> history(String key, Authentication authentication) {
        PlatformSettingCatalog.Definition definition = definition(key);
        if (!mayAccess(definition.domain(), authentication)) {
            throw new ForbiddenException("Ce paramètre est réservé au super administrateur.");
        }
        List<ChangeView> changes = new ArrayList<>();
        auditRepository.findBySettingKeyOrderByChangedAtDesc(key, PageRequest.of(0, HISTORY_SIZE))
                .forEach(log -> changes.add(new ChangeView(log.getOldValue(), log.getNewValue(), log.getReason(), log.getChangedAt())));
        return changes;
    }

    private PlatformSettingCatalog.Definition editable(String key, Authentication authentication) {
        PlatformSettingCatalog.Definition definition = definition(key);
        if (!mayAccess(definition.domain(), authentication)) {
            throw new ForbiddenException("Ce paramètre est réservé au super administrateur.");
        }
        return definition;
    }

    private static PlatformSettingCatalog.Definition definition(String key) {
        return PlatformSettingCatalog.find(key)
                .orElseThrow(() -> new ResourceNotFoundException("Paramètre inconnu : " + key));
    }

    static boolean mayAccess(SettingDomain domain, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        return roles.contains(SUPER_ADMIN) || (roles.contains(ADMIN) && domain.adminEditable());
    }

    private void audit(String key, String oldValue, String newValue, String reason, Authentication authentication) {
        String actor = authentication.getName();
        auditRepository.save(SettingsAuditLogs.builder()
                .settingKey(key)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(clock.instant())
                .reason(StringUtils.hasText(reason) ? actor + " · " + reason.trim() : actor)
                .build());
    }

    private void refreshAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            settings.refresh();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                settings.refresh();
            }
        });
    }

    private SettingView view(PlatformSettingCatalog.Definition definition, PlatformSetting stored) {
        boolean secret = definition.type() == PlatformSettingCatalog.Type.SECRET;
        if (secret) {
            return new SettingView(definition.key(), definition.label(), definition.domain(), definition.type(), definition.effect(),
                    null, null, false, false, StringUtils.hasText(fileValue(definition.key())), false, null, null);
        }
        String fallback = defaultValue(definition);
        String value = stored != null ? stored.getValue() : fallback;
        return new SettingView(definition.key(), definition.label(), definition.domain(), definition.type(), definition.effect(),
                value, fallback, stored != null, true, null, restartPending(definition, value),
                stored == null ? null : stored.getUpdatedBy(), stored == null ? null : stored.getUpdatedAt());
    }

    /** La valeur voulue n'est pas celle que l'application a lue en demarrant. */
    private boolean restartPending(PlatformSettingCatalog.Definition definition, String value) {
        if (definition.effect() != PlatformSettingCatalog.Effect.RESTART) {
            return false;
        }
        String running = environment.getProperty(definition.key(), definition.defaultValue());
        return !Objects.equals(value, running);
    }

    private String defaultValue(PlatformSettingCatalog.Definition definition) {
        String value = fileValue(definition.key());
        return value != null ? value : definition.defaultValue();
    }

    /** La configuration sans les valeurs enregistrees · ce qu'une remise a zero retrouverait. */
    private String fileValue(String key) {
        MutablePropertySources sources = new MutablePropertySources(environment.getPropertySources());
        sources.remove(PlatformSettingsEnvironmentPostProcessor.SOURCE_NAME);
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(sources);
        resolver.setIgnoreUnresolvableNestedPlaceholders(true);
        return resolver.getProperty(key);
    }
}
