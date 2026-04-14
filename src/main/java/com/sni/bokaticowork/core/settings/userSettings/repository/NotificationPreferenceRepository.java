package com.sni.bokaticowork.core.settings.userSettings.repository;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.settings.userSettings.model.NotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferences, Long> {

    List<NotificationPreferences> findAllByMember_IdOrderByEventTypeAscNotificationChannelAsc(Long memberId);

    Optional<NotificationPreferences> findByMember_IdAndEventTypeAndNotificationChannel(Long memberId,
                                                                                        EventType eventType,
                                                                                        NotificationChannel notificationChannel);

}
