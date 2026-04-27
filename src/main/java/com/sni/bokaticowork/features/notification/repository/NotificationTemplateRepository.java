package com.sni.bokaticowork.features.notification.repository;

import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateCodeIgnoreCase(String templateCode);

    Optional<NotificationTemplate> findByTemplateCodeIgnoreCaseAndChannelAndActiveTrue(String templateCode,
                                                                                       NotificationChannel channel);

    @Query(value = """
            SELECT *
            FROM notification_template
            WHERE (CAST(:channel AS varchar) IS NULL OR channel = CAST(:channel AS varchar))
              AND active = COALESCE(CAST(:active AS boolean), active)
            ORDER BY template_code ASC
            """, nativeQuery = true)
    List<NotificationTemplate> list(String channel, Boolean active);
}
