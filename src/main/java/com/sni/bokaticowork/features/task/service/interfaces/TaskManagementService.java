package com.sni.bokaticowork.features.task.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.task.dto.TaskDtos.*;
import com.sni.bokaticowork.features.task.enums.TaskPriority;
import com.sni.bokaticowork.features.task.enums.TaskStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskManagementService {
    TaskResponse create(CreateTaskRequest request);
    PaginatedResponse<TaskResponse> search(TaskStatus status, Long assignedTo, TaskPriority priority,
                                           String sourceType, String searchText, Pageable pageable);
    TaskResponse get(Long id);
    TaskResponse update(Long id, UpdateTaskRequest request);
    TaskResponse complete(Long id);
    TaskResponse archive(Long id);
    TaskResponse addChecklistItem(Long taskId, ChecklistRequest request);
    TaskResponse updateChecklistItem(Long taskId, Long itemId, UpdateChecklistItemRequest request);
    TaskResponse removeChecklistItem(Long taskId, Long itemId);
    TaskResponse updateStatus(Long id, UpdateTaskStatusRequest request);
    TaskResponse assign(Long id, AssignTaskRequest request);
    TaskResponse addComment(Long id, AddTaskCommentRequest request);
    List<TaskResponse> dueToday();
    TaskResponse createFromAutomation(CreateTaskRequest request);
    TaskKanbanBoardResponse getBoard(Long assignedTo, TaskPriority priority,
                                     String sourceType, String searchText);
    TaskMetricsResponse metrics();
}
