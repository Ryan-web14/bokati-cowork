package com.sni.bokaticowork.features.task.repository;

import com.sni.bokaticowork.features.task.enums.TaskStatus;
import com.sni.bokaticowork.features.task.model.TaskItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
    Page<TaskItem> findAllByAssignedTo(Long assignedTo, Pageable pageable);
    List<TaskItem> findAllByDueAtBetweenAndStatusNot(Instant start, Instant end, TaskStatus status);
}
