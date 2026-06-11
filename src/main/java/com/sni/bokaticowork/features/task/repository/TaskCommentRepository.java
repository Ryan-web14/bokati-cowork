package com.sni.bokaticowork.features.task.repository;

import com.sni.bokaticowork.features.task.model.TaskComment;
import com.sni.bokaticowork.features.task.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findAllByTaskOrderByCreatedAtAsc(TaskItem task);

    @Query("SELECT c.task.id, COUNT(c) FROM TaskComment c WHERE c.task.id IN :taskIds GROUP BY c.task.id")
    List<Object[]> countByTaskIds(@Param("taskIds") List<Long> taskIds);
}
