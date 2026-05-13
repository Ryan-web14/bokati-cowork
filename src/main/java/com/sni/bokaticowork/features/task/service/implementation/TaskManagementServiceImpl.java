package com.sni.bokaticowork.features.task.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.richtext.RichTextSupport;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.task.dto.TaskDtos.*;
import com.sni.bokaticowork.features.task.enums.*;
import com.sni.bokaticowork.features.task.mapper.TaskMapper;
import com.sni.bokaticowork.features.task.model.*;
import com.sni.bokaticowork.features.task.repository.*;
import com.sni.bokaticowork.features.task.service.interfaces.TaskManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskManagementServiceImpl implements TaskManagementService {
    private final TaskItemRepository taskRepository;
    private final TaskChecklistRepository checklistRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskMapper mapper;
    private final RichTextSupport richTextSupport;

    @Override
    public TaskResponse create(CreateTaskRequest request) {
        return createFromAutomation(request);
    }

    @Override
    public TaskResponse createFromAutomation(CreateTaskRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Task title is required");
        }
        TaskItem task = taskRepository.save(TaskItem.builder()
                .title(request.title())
                .description(richTextSupport.normalize(request.description()))
                .assignedTo(request.assignedTo())
                .status(TaskStatus.OPEN)
                .priority(request.priority() == null ? TaskPriority.MEDIUM : request.priority())
                .dueAt(request.dueAt())
                .sourceType(request.sourceType())
                .sourceCode(request.sourceCode())
                .recurrence(request.recurrence() == null ? TaskRecurrence.NONE : request.recurrence())
                .build());
        if (request.checklist() != null) {
            request.checklist().forEach(item -> checklistRepository.save(TaskChecklist.builder()
                    .task(task)
                    .label(item.label())
                    .completed(Boolean.TRUE.equals(item.completed()))
                    .displayOrder(item.displayOrder())
                    .build()));
        }
        return response(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<TaskResponse> list(Long assignedTo, Pageable pageable) {
        return new PaginatedResponse<>((assignedTo == null ? taskRepository.findAll(pageable) : taskRepository.findAllByAssignedTo(assignedTo, pageable))
                .map(this::response));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return response(getTask(id));
    }

    @Override
    public TaskResponse complete(Long id) {
        TaskItem task = getTask(id);
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        return response(taskRepository.save(task));
    }

    @Override
    public TaskResponse assign(Long id, AssignTaskRequest request) {
        TaskItem task = getTask(id);
        task.setAssignedTo(request == null ? null : request.assignedTo());
        if (task.getStatus() == TaskStatus.OPEN) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }
        return response(taskRepository.save(task));
    }

    @Override
    public TaskResponse addComment(Long id, AddTaskCommentRequest request) {
        if (request == null || !StringUtils.hasText(request.comment())) {
            throw new BadRequestException("Comment is required");
        }
        TaskItem task = getTask(id);
        commentRepository.save(TaskComment.builder()
                .task(task)
                .authorId(request.authorId())
                .authorName(request.authorName())
                .comment(richTextSupport.normalize(request.comment()))
                .build());
        return response(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> dueToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return taskRepository.findAllByDueAtBetweenAndStatusNot(
                        today.atStartOfDay().toInstant(ZoneOffset.UTC),
                        today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                        TaskStatus.COMPLETED)
                .stream()
                .map(this::response)
                .toList();
    }

    private TaskItem getTask(Long id) {
        if (id == null) {
            throw new BadRequestException("Task id is required");
        }
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private TaskResponse response(TaskItem task) {
        return mapper.toResponse(task,
                checklistRepository.findAllByTaskOrderByDisplayOrderAscIdAsc(task),
                commentRepository.findAllByTaskOrderByCreatedAtAsc(task));
    }
}
