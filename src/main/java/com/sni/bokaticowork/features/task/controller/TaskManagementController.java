package com.sni.bokaticowork.features.task.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.task.dto.TaskDtos.*;
import com.sni.bokaticowork.features.task.enums.TaskPriority;
import com.sni.bokaticowork.features.task.service.interfaces.TaskManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/tasks")
public class TaskManagementController {
    private final TaskManagementService service;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('TASK:WRITE','TASK_WRITE')")
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('TASK:READ','TASK_READ')")
    public ResponseEntity<PaginatedResponse<TaskResponse>> list(@RequestParam(required = false) Long assignedTo,
                                                                @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.list(assignedTo, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('TASK:READ','TASK_READ')")
    public ResponseEntity<TaskResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('TASK:WRITE','TASK_WRITE')")
    public ResponseEntity<TaskResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(service.complete(id));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyAuthority('TASK:ASSIGN','TASK_ASSIGN')")
    public ResponseEntity<TaskResponse> assign(@PathVariable Long id, @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(service.assign(id, request));
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyAuthority('TASK:WRITE','TASK_WRITE')")
    public ResponseEntity<TaskResponse> comment(@PathVariable Long id, @Valid @RequestBody AddTaskCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addComment(id, request));
    }

    @GetMapping("/due-today")
    @PreAuthorize("hasAnyAuthority('TASK:READ','TASK_READ')")
    public ResponseEntity<List<TaskResponse>> dueToday() {
        return ResponseEntity.ok(service.dueToday());
    }

    @GetMapping("/board")
    @PreAuthorize("hasAnyAuthority('TASK:READ','TASK_READ')")
    public ResponseEntity<TaskKanbanBoardResponse> board(
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String searchText) {
        return ResponseEntity.ok(service.getBoard(assignedTo, priority, sourceType, searchText));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('TASK:WRITE','TASK_WRITE')")
    public ResponseEntity<TaskResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(service.updateStatus(id, request));
    }
}
