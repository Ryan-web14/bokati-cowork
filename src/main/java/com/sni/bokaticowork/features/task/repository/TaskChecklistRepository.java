package com.sni.bokaticowork.features.task.repository;

import com.sni.bokaticowork.features.task.model.TaskChecklist;
import com.sni.bokaticowork.features.task.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskChecklistRepository extends JpaRepository<TaskChecklist, Long> {
    List<TaskChecklist> findAllByTaskOrderByDisplayOrderAscIdAsc(TaskItem task);

    @Query("""
            SELECT c.task.id, COUNT(c),
                   SUM(CASE WHEN c.completed = true THEN 1L ELSE 0L END)
            FROM TaskChecklist c
            WHERE c.task.id IN :taskIds
            GROUP BY c.task.id
            """)
    List<Object[]> countByTaskIds(@Param("taskIds") List<Long> taskIds);
}
