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
}
