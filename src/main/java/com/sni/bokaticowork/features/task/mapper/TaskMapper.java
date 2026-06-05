package com.sni.bokaticowork.features.task.mapper;

import com.sni.bokaticowork.features.task.dto.TaskDtos.*;
import com.sni.bokaticowork.features.task.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskMapper {
    public TaskResponse toResponse(TaskItem task, List<TaskChecklist> checklist, List<TaskComment> comments) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssignedTo(),
                task.getStatus(),
                task.getPriority(),
                task.getDueAt(),
                task.getSourceType(),
                task.getSourceCode(),
                task.getRecurrence(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                checklist == null ? List.of() : checklist.stream().map(this::toChecklistResponse).toList(),
                comments == null ? List.of() : comments.stream().map(this::toCommentResponse).toList()
        );
    }

    public ChecklistResponse toChecklistResponse(TaskChecklist item) {
        return new ChecklistResponse(item.getId(), item.getLabel(), item.getCompleted(), item.getDisplayOrder());
    }

    public TaskCommentResponse toCommentResponse(TaskComment comment) {
        return new TaskCommentResponse(comment.getId(), comment.getAuthorId(), comment.getAuthorName(), comment.getComment(), comment.getCreatedAt());
    }

    public TaskKanbanCardResponse toKanbanCard(TaskItem task, int checklistTotal,
                                               int checklistDone, int commentCount) {
        boolean overdue = task.getDueAt() != null
                && task.getDueAt().isBefore(java.time.Instant.now())
                && task.getStatus() != com.sni.bokaticowork.features.task.enums.TaskStatus.COMPLETED
                && task.getStatus() != com.sni.bokaticowork.features.task.enums.TaskStatus.CANCELLED;
        return new TaskKanbanCardResponse(
                task.getId(),
                task.getTitle(),
                task.getAssignedTo(),
                task.getStatus(),
                task.getPriority(),
                task.getRecurrence(),
                task.getSourceType(),
                task.getSourceCode(),
                task.getDueAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                checklistTotal,
                checklistDone,
                commentCount,
                overdue
        );
    }
}
