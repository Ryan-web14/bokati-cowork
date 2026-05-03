package com.sni.bokaticowork.features.task.model;

import com.sni.bokaticowork.features.task.enums.TaskPriority;
import com.sni.bokaticowork.features.task.enums.TaskRecurrence;
import com.sni.bokaticowork.features.task.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "task_item")
public class TaskItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private Long assignedTo;
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    @Enumerated(EnumType.STRING)
    private TaskPriority priority;
    private Instant dueAt;
    private String sourceType;
    private String sourceCode;
    @Enumerated(EnumType.STRING)
    private TaskRecurrence recurrence;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
