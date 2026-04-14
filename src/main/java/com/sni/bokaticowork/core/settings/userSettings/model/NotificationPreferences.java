package com.sni.bokaticowork.core.settings.userSettings.model;

import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferences {

    @Id
    @IdGeneration
    @Column(name = "id")
    private long id;

    @ManyToOne
    @JoinColumn(name = "member_id",nullable = false, foreignKey = @ForeignKey(name = "fk_member_notification"))
    private Member member;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType  eventType;

    @Column(name = "notification_channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel notificationChannel;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "preference_name")
    private String preferenceName;

    @Column(name = "preference_description")
    private String preferenceDescription;

}
