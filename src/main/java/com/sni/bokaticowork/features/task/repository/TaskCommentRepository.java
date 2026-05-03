package com.sni.bokaticowork.features.task.repository;

import com.sni.bokaticowork.features.task.model.TaskComment;
import com.sni.bokaticowork.features.task.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findAllByTaskOrderByCreatedAtAsc(TaskItem task);
}
