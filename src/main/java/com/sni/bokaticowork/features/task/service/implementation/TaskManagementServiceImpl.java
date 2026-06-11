package com.sni.bokaticowork.features.task.service.implementation;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.richtext.RichTextSupport;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.task.dto.TaskDtos.*;
import com.sni.bokaticowork.features.task.enums.*;
import com.sni.bokaticowork.features.task.mapper.TaskMapper;
import com.sni.bokaticowork.features.task.model.*;
import com.sni.bokaticowork.features.task.repository.*;
import com.sni.bokaticowork.features.task.service.interfaces.TaskEmailService;
import com.sni.bokaticowork.features.task.service.interfaces.TaskManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskManagementServiceImpl implements TaskManagementService {
    private final TaskItemRepository taskRepository;
    private final TaskChecklistRepository checklistRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskMapper mapper;
    private final RichTextSupport richTextSupport;
    private final TaskEmailService emailService;

    @Override
    @Audited(module = "TASK", action = "CREATE", ressource = "task_item")
    public TaskResponse create(CreateTaskRequest request) {
        return createFromAutomation(request);
    }

    @Override
    @Audited(module = "TASK", action = "CREATE_FROM_AUTOMATION", ressource = "task_item")
    public TaskResponse createFromAutomation(CreateTaskRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Task title is required");
        }
        TaskItem task = taskRepository.save(TaskItem.builder()
                .title(request.title().trim())
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
            for (ChecklistRequest item : request.checklist()) {
                if (item == null || !StringUtils.hasText(item.label())) {
                    throw new BadRequestException("Checklist item label is required");
                }
                checklistRepository.save(TaskChecklist.builder()
                        .task(task)
                        .label(item.label().trim())
                        .completed(Boolean.TRUE.equals(item.completed()))
                        .displayOrder(item.displayOrder())
                        .build());
            }
        }
        if (task.getAssignedTo() != null) {
            emailService.sendTaskAssigned(task);
        }
        return response(task);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<TaskResponse> search(TaskStatus status, Long assignedTo, TaskPriority priority,
                                                  String sourceType, String searchText, Pageable pageable) {
        String statusStr   = status   != null ? status.name()   : null;
        String priorityStr = priority != null ? priority.name() : null;
        String srcType     = StringUtils.hasText(sourceType) ? sourceType.trim() : null;
        String text        = StringUtils.hasText(searchText) ? searchText.trim() : null;
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(
                taskRepository.search(statusStr, assignedTo, priorityStr, srcType, text, unsortedPageable)
                        .map(this::response));
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return response(getTask(id));
    }

    @Override
    @Audited(module = "TASK", action = "UPDATE", ressource = "task_item")
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        if (request == null) {
            throw new BadRequestException("Update payload is required");
        }
        TaskItem task = getTask(id);
        if (request.title() != null) {
            if (!StringUtils.hasText(request.title())) {
                throw new BadRequestException("Task title is required");
            }
            task.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            task.setDescription(richTextSupport.normalize(request.description()));
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueAt() != null) {
            if (!Objects.equals(task.getDueAt(), request.dueAt())) {
                resetReminderAlerts(task);
            }
            task.setDueAt(request.dueAt());
        }
        if (request.sourceType() != null) {
            task.setSourceType(normalizeOptionalText(request.sourceType()));
        }
        if (request.sourceCode() != null) {
            task.setSourceCode(normalizeOptionalText(request.sourceCode()));
        }
        if (request.recurrence() != null) {
            task.setRecurrence(request.recurrence());
        }
        return response(taskRepository.save(task));
    }

    @Override
    @Audited(module = "TASK", action = "COMPLETE", ressource = "task_item")
    public TaskResponse complete(Long id) {
        TaskItem task = getTask(id);
        return response(completeTask(task));
    }

    @Override
    @Audited(module = "TASK", action = "ARCHIVE", ressource = "task_item")
    public TaskResponse archive(Long id) {
        TaskItem task = getTask(id);
        task.setStatus(TaskStatus.ARCHIVED);
        task.setCompletedAt(null);
        return response(taskRepository.save(task));
    }

    @Override
    @Audited(module = "TASK", action = "UPDATE_STATUS", ressource = "task_item")
    public TaskResponse updateStatus(Long id, UpdateTaskStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BadRequestException("Status is required");
        }
        TaskItem task = getTask(id);
        TaskStatus previousStatus = task.getStatus();
        if (request.status() == TaskStatus.COMPLETED) {
            return response(completeTask(task));
        }
        task.setStatus(request.status());
        task.setCompletedAt(null);
        if (previousStatus != request.status() && isActiveStatus(request.status())) {
            resetReminderAlerts(task);
        }
        return response(taskRepository.save(task));
    }

    @Override
    @Audited(module = "TASK", action = "ASSIGN", ressource = "task_item")
    public TaskResponse assign(Long id, AssignTaskRequest request) {
        TaskItem task = getTask(id);
        Long previousAssignee = task.getAssignedTo();
        task.setAssignedTo(request == null ? null : request.assignedTo());
        if (task.getStatus() == TaskStatus.OPEN) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }
        TaskItem saved = taskRepository.save(task);
        if (saved.getAssignedTo() != null && !Objects.equals(previousAssignee, saved.getAssignedTo())) {
            emailService.sendTaskAssigned(saved);
        }
        return response(saved);
    }

    @Override
    @Audited(module = "TASK", action = "ADD_COMMENT", ressource = "task_comment")
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
        return response(touch(task));
    }

    @Override
    @Audited(module = "TASK", action = "ADD_CHECKLIST_ITEM", ressource = "task_checklist")
    public TaskResponse addChecklistItem(Long taskId, ChecklistRequest request) {
        if (request == null || !StringUtils.hasText(request.label())) {
            throw new BadRequestException("Checklist item label is required");
        }
        TaskItem task = getTask(taskId);
        checklistRepository.save(TaskChecklist.builder()
                .task(task)
                .label(request.label().trim())
                .completed(Boolean.TRUE.equals(request.completed()))
                .displayOrder(request.displayOrder())
                .build());
        return response(touch(task));
    }

    @Override
    @Audited(module = "TASK", action = "UPDATE_CHECKLIST_ITEM", ressource = "task_checklist")
    public TaskResponse updateChecklistItem(Long taskId, Long itemId, UpdateChecklistItemRequest request) {
        if (request == null) {
            throw new BadRequestException("Update payload is required");
        }
        TaskItem task = getTask(taskId);
        TaskChecklist item = checklistRepository.findByIdAndTask_Id(itemId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));
        if (request.label() != null) {
            if (!StringUtils.hasText(request.label())) {
                throw new BadRequestException("Checklist item label is required");
            }
            item.setLabel(request.label().trim());
        }
        if (request.completed() != null) {
            item.setCompleted(request.completed());
        }
        if (request.displayOrder() != null) {
            item.setDisplayOrder(request.displayOrder());
        }
        checklistRepository.save(item);
        return response(touch(task));
    }

    @Override
    @Audited(module = "TASK", action = "REMOVE_CHECKLIST_ITEM", ressource = "task_checklist")
    public TaskResponse removeChecklistItem(Long taskId, Long itemId) {
        TaskItem task = getTask(taskId);
        TaskChecklist item = checklistRepository.findByIdAndTask_Id(itemId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found: " + itemId));
        checklistRepository.delete(item);
        return response(touch(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> dueToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return taskRepository.findAllByDueAtBetweenAndStatusNotIn(
                        today.atStartOfDay().toInstant(ZoneOffset.UTC),
                        today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                        List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED))
                .stream()
                .map(this::response)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskKanbanBoardResponse getBoard(Long assignedTo, TaskPriority priority,
                                            String sourceType, String searchText) {
        String priorityStr = priority != null ? priority.name() : null;
        String text        = StringUtils.hasText(searchText) ? searchText.trim() : null;
        String srcType     = StringUtils.hasText(sourceType) ? sourceType.trim() : null;

        List<TaskKanbanColumnResponse> columns = new ArrayList<>();
        long totalTasks  = 0;
        long overdueTasks = 0;

        for (TaskStatus status : Arrays.asList(TaskStatus.OPEN, TaskStatus.IN_PROGRESS,
                                               TaskStatus.COMPLETED, TaskStatus.CANCELLED)) {
            List<TaskItem> tasks = taskRepository.findByStatusFiltered(
                    status.name(), assignedTo, priorityStr, srcType, text);

            if (tasks.isEmpty()) {
                columns.add(new TaskKanbanColumnResponse(status, 0, List.of()));
                continue;
            }

            List<Long> ids = tasks.stream().map(TaskItem::getId).toList();

            Map<Long, int[]> checklistCounts = checklistRepository.countByTaskIds(ids).stream()
                    .collect(Collectors.toMap(
                            row -> ((Number) row[0]).longValue(),
                            row -> new int[]{((Number) row[1]).intValue(), ((Number) row[2]).intValue()}));

            Map<Long, Integer> commentCounts = commentRepository.countByTaskIds(ids).stream()
                    .collect(Collectors.toMap(
                            row -> ((Number) row[0]).longValue(),
                            row -> ((Number) row[1]).intValue()));

            List<TaskKanbanCardResponse> cards = tasks.stream().map(t -> {
                int[] cl = checklistCounts.getOrDefault(t.getId(), new int[]{0, 0});
                int   cc = commentCounts.getOrDefault(t.getId(), 0);
                return mapper.toKanbanCard(t, cl[0], cl[1], cc);
            }).toList();

            long colOverdue = cards.stream().filter(TaskKanbanCardResponse::overdue).count();
            columns.add(new TaskKanbanColumnResponse(status, tasks.size(), cards));
            totalTasks  += tasks.size();
            overdueTasks += colOverdue;
        }

        return new TaskKanbanBoardResponse(columns, totalTasks, overdueTasks);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskMetricsResponse metrics() {
        long total = taskRepository.count();
        long open = taskRepository.countByStatus(TaskStatus.OPEN);
        long inProgress = taskRepository.countByStatus(TaskStatus.IN_PROGRESS);
        long completed = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long cancelled = taskRepository.countByStatus(TaskStatus.CANCELLED);
        long archived = taskRepository.countByStatus(TaskStatus.ARCHIVED);
        long overdue = taskRepository.countOverdue(Instant.now(),
                List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED, TaskStatus.ARCHIVED));

        double completionRate = total > 0
                ? Math.round((double) completed / total * 1000.0) / 10.0
                : 0.0;
        Double avgHours = taskRepository.avgCompletionHours();

        return new TaskMetricsResponse(
                total,
                open,
                inProgress,
                completed,
                cancelled,
                archived,
                overdue,
                completionRate,
                avgHours != null ? Math.round(avgHours * 10.0) / 10.0 : 0.0,
                toCountMap(taskRepository.countByPriority()),
                toCountMap(taskRepository.countBySourceType())
        );
    }

    private TaskItem getTask(Long id) {
        if (id == null) {
            throw new BadRequestException("Task id is required");
        }
        return taskRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    private TaskItem touch(TaskItem task) {
        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean isActiveStatus(TaskStatus status) {
        return status != null
                && status != TaskStatus.COMPLETED
                && status != TaskStatus.CANCELLED
                && status != TaskStatus.ARCHIVED;
    }

    private void resetReminderAlerts(TaskItem task) {
        task.setDueSoonAlertSentAt(null);
        task.setOverdueAlertSentAt(null);
    }

    private TaskItem completeTask(TaskItem task) {
        boolean firstCompletion = task.getStatus() != TaskStatus.COMPLETED;
        task.setStatus(TaskStatus.COMPLETED);
        if (task.getCompletedAt() == null) {
            task.setCompletedAt(Instant.now());
        }
        TaskItem saved = taskRepository.save(task);
        if (firstCompletion) {
            regenerateRecurringTask(saved);
        }
        return saved;
    }

    private void regenerateRecurringTask(TaskItem completedTask) {
        Instant nextDueAt = nextDueAt(completedTask);
        if (nextDueAt == null || taskRepository.existsByParentTaskId(completedTask.getId())) {
            return;
        }

        TaskItem nextTask = taskRepository.save(TaskItem.builder()
                .title(completedTask.getTitle())
                .description(completedTask.getDescription())
                .assignedTo(completedTask.getAssignedTo())
                .status(TaskStatus.OPEN)
                .priority(completedTask.getPriority())
                .dueAt(nextDueAt)
                .sourceType(completedTask.getSourceType())
                .sourceCode(completedTask.getSourceCode())
                .recurrence(completedTask.getRecurrence())
                .parentTaskId(completedTask.getId())
                .build());

        checklistRepository.findAllByTaskOrderByDisplayOrderAscIdAsc(completedTask)
                .forEach(item -> checklistRepository.save(TaskChecklist.builder()
                        .task(nextTask)
                        .label(item.getLabel())
                        .completed(false)
                        .displayOrder(item.getDisplayOrder())
                        .build()));

        if (nextTask.getAssignedTo() != null) {
            emailService.sendTaskAssigned(nextTask);
        }
    }

    private Instant nextDueAt(TaskItem completedTask) {
        if (completedTask.getDueAt() == null || completedTask.getRecurrence() == null) {
            return null;
        }
        return switch (completedTask.getRecurrence()) {
            case DAILY -> completedTask.getDueAt().plus(1, ChronoUnit.DAYS);
            case WEEKLY -> completedTask.getDueAt().plus(7, ChronoUnit.DAYS);
            case MONTHLY -> completedTask.getDueAt().atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
            case NONE, AFTER_BOOKING -> null;
        };
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                result.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        return result;
    }

    private TaskResponse response(TaskItem task) {
        return mapper.toResponse(task,
                checklistRepository.findAllByTaskOrderByDisplayOrderAscIdAsc(task),
                commentRepository.findAllByTaskOrderByCreatedAtAsc(task));
    }
}
