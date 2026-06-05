package com.sni.bokaticowork.features.task.repository;

import com.sni.bokaticowork.features.task.enums.TaskStatus;
import com.sni.bokaticowork.features.task.model.TaskItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TaskItemRepository extends JpaRepository<TaskItem, Long> {
    Page<TaskItem> findAllByAssignedTo(Long assignedTo, Pageable pageable);
    List<TaskItem> findAllByDueAtBetweenAndStatusNot(Instant start, Instant end, TaskStatus status);

    @Query(nativeQuery = true, value = """
            SELECT t.*
            FROM task_item t
            WHERE t.status = CAST(:status AS TEXT)
              AND (CAST(:assignedTo AS BIGINT) IS NULL OR t.assigned_to = CAST(:assignedTo AS BIGINT))
              AND (CAST(:priority AS TEXT) IS NULL OR t.priority = CAST(:priority AS TEXT))
              AND (CAST(:sourceType AS TEXT) IS NULL OR t.source_type = CAST(:sourceType AS TEXT))
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(t.title) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(t.description, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(COALESCE(t.source_code, '')) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
              )
            ORDER BY
              CASE t.priority
                WHEN 'URGENT' THEN 1
                WHEN 'HIGH'   THEN 2
                WHEN 'MEDIUM' THEN 3
                WHEN 'LOW'    THEN 4
                ELSE 5
              END,
              COALESCE(t.due_at, t.created_at) ASC
            """)
    List<TaskItem> findByStatusFiltered(@Param("status") String status,
                                        @Param("assignedTo") Long assignedTo,
                                        @Param("priority") String priority,
                                        @Param("sourceType") String sourceType,
                                        @Param("searchText") String searchText);
}
