package com.sni.bokaticowork.features.task.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.task.dto.TaskDtos.*;
import com.sni.bokaticowork.features.task.enums.TaskPriority;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskManagementService {
    TaskResponse create(CreateTaskRequest request);
    PaginatedResponse<TaskResponse> list(Long assignedTo, Pageable pageable);
    TaskResponse get(Long id);
    TaskResponse complete(Long id);
    TaskResponse updateStatus(Long id, UpdateTaskStatusRequest request);
    TaskResponse assign(Long id, AssignTaskRequest request);
    TaskResponse addComment(Long id, AddTaskCommentRequest request);
    List<TaskResponse> dueToday();
    TaskResponse createFromAutomation(CreateTaskRequest request);
    TaskKanbanBoardResponse getBoard(Long assignedTo, TaskPriority priority,
                                     String sourceType, String searchText);
}
