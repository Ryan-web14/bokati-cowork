package com.sni.bokaticowork.core.audit.change;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Ecouteurs Hibernate du journal des actions (lot A1) : chaque insertion, modification et suppression
 * ecrite en base pendant une action auditee est versee dans {@link AuditChanges}, avec l'avant et
 * l'apres des champs modifies pour les entites principales.
 *
 * <p>Hors d'une action auditee, les ecouteurs ne font rien. Une erreur de capture ne remonte jamais :
 * l'audit ne fait pas echouer l'operation.</p>
 */
@Slf4j
@Component
public class EntityChangeListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final EntityManagerFactory entityManagerFactory;

    public EntityChangeListener(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    void register() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class).getEventListenerRegistry();
        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
        registry.appendListeners(EventType.POST_DELETE, this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        capture("CREATE", event.getEntity(), event.getPersister(), null, event.getState(), null);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        capture("UPDATE", event.getEntity(), event.getPersister(), event.getOldState(), event.getState(), event.getDirtyProperties());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        capture("DELETE", event.getEntity(), event.getPersister(), null, null, null);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    void capture(String operation, Object entity, EntityPersister persister, Object[] oldState, Object[] state, int[] dirty) {
        if (!AuditChanges.active() || entity == null) {
            return;
        }
        try {
            Class<?> type = entity.getClass();
            if (!AuditedEntities.isTracked(type)) {
                return;
            }
            boolean main = AuditedEntities.isMain(type);
            String code = AuditedEntities.codeOf(entity);
            List<AuditChanges.Change> targets = AuditChanges.record(type.getSimpleName(), code, operation, main);
            if (!main || targets.isEmpty() || state == null) {
                return;
            }
            String[] names = persister.getPropertyNames();
            // Parametre dont la cle designe un secret (cle d'API, mot de passe...) : valeurs jamais ecrites.
            boolean maskAll = "PlatformSetting".equals(type.getSimpleName()) && code != null && AuditValues.isSensitive(code);
            if ("CREATE".equals(operation)) {
                for (int i = 0; i < names.length; i++) {
                    if (state[i] != null && !AuditValues.isIgnored(names[i]) && !isCollection(state[i])) {
                        AuditChanges.field(targets, names[i], null, maskAll ? AuditValues.MASK : AuditValues.render(names[i], state[i]));
                    }
                }
                return;
            }
            int[] indexes = dirty != null ? dirty : allIndexes(names.length);
            for (int i : indexes) {
                String name = names[i];
                Object before = oldState == null ? null : oldState[i];
                Object after = state[i];
                if (AuditValues.isIgnored(name) || isCollection(after) || isCollection(before)) {
                    continue;
                }
                Object renderedBefore = maskAll && before != null ? AuditValues.MASK : AuditValues.render(name, before);
                Object renderedAfter = maskAll && after != null ? AuditValues.MASK : AuditValues.render(name, after);
                if (oldState != null && Objects.equals(renderedBefore, renderedAfter) && !AuditValues.MASK.equals(renderedAfter)) {
                    continue;
                }
                AuditChanges.field(targets, name, oldState == null ? "?" : renderedBefore, renderedAfter);
            }
        } catch (RuntimeException exception) {
            log.warn("Journal des actions : changement non capture ({}) : {}", entity.getClass().getSimpleName(), exception.getMessage());
        }
    }

    /** Associations multiples (listes d'entites liees) : suivies par leurs propres entites, pas ici. */
    private static boolean isCollection(Object value) {
        return value instanceof org.hibernate.collection.spi.PersistentCollection<?>;
    }

    private static int[] allIndexes(int count) {
        int[] indexes = new int[count];
        for (int i = 0; i < count; i++) {
            indexes[i] = i;
        }
        return indexes;
    }
}
