package com.sni.bokaticowork.features.task.dto;

import com.sni.bokaticowork.features.task.enums.TaskPriority;
import com.sni.bokaticowork.features.task.enums.TaskRecurrence;
import com.sni.bokaticowork.features.task.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class TaskDtos {
    public record ChecklistRequest(String label, Boolean completed, Integer displayOrder) {}

    public record CreateTaskRequest(
            @NotBlank String title,
            String description,
            Long assignedTo,
            TaskPriority priority,
            Instant dueAt,
            String sourceType,
            String sourceCode,
            TaskRecurrence recurrence,
            List<ChecklistRequest> checklist
    ) {}

    public record AssignTaskRequest(Long assignedTo) {}
    public record AddTaskCommentRequest(String authorId, String authorName, @NotBlank String comment) {}

    public record ChecklistResponse(Long id, String label, Boolean completed, Integer displayOrder) {}
    public record TaskCommentResponse(Long id, String authorId, String authorName, String comment, Instant createdAt) {}

    public record TaskResponse(
            Long id,
            String title,
            String description,
            Long assignedTo,
            TaskStatus status,
            TaskPriority priority,
            Instant dueAt,
            String sourceType,
            String sourceCode,
            TaskRecurrence recurrence,
            Instant completedAt,
            Instant createdAt,
            List<ChecklistResponse> checklist,
            List<TaskCommentResponse> comments
    ) {}
}
