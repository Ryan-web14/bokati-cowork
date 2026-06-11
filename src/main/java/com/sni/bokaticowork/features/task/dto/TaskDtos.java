package com.sni.bokaticowork.features.task.dto;

import com.sni.bokaticowork.features.task.enums.TaskPriority;
import com.sni.bokaticowork.features.task.enums.TaskRecurrence;
import com.sni.bokaticowork.features.task.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class TaskDtos {
    public record ChecklistRequest(String label, Boolean completed, Integer displayOrder) {}

    public record UpdateChecklistItemRequest(String label, Boolean completed, Integer displayOrder) {}

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

    public record UpdateTaskRequest(
            String title,
            String description,
            TaskPriority priority,
            Instant dueAt,
            String sourceType,
            String sourceCode,
            TaskRecurrence recurrence
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
            Long parentTaskId,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt,
            List<ChecklistResponse> checklist,
            List<TaskCommentResponse> comments
    ) {}

    public record UpdateTaskStatusRequest(
            @jakarta.validation.constraints.NotNull TaskStatus status
    ) {}

    // ── Kanban Board ──────────────────────────────────────────────

    public record TaskKanbanCardResponse(
            Long id,
            String title,
            Long assignedTo,
            TaskStatus status,
            TaskPriority priority,
            TaskRecurrence recurrence,
            Long parentTaskId,
            String sourceType,
            String sourceCode,
            Instant dueAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt,
            int checklistTotal,
            int checklistDone,
            int commentCount,
            boolean overdue
    ) {}

    public record TaskKanbanColumnResponse(
            TaskStatus status,
            long count,
            List<TaskKanbanCardResponse> cards
    ) {}

    public record TaskKanbanBoardResponse(
            List<TaskKanbanColumnResponse> columns,
            long totalTasks,
            long overdueTasks
    ) {}

    // ── Metrics ──────────────────────────────────────────────────

    public record TaskMetricsResponse(
            long totalTasks,
            long openTasks,
            long inProgressTasks,
            long completedTasks,
            long cancelledTasks,
            long archivedTasks,
            long overdueTasks,
            double completionRate,
            double avgCompletionHours,
            Map<String, Long> byPriority,
            Map<String, Long> bySourceType
    ) {}
}
