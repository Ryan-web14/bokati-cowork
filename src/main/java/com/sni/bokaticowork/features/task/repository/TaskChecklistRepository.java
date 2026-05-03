package com.sni.bokaticowork.features.task.repository;

import com.sni.bokaticowork.features.task.model.TaskChecklist;
import com.sni.bokaticowork.features.task.model.TaskItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskChecklistRepository extends JpaRepository<TaskChecklist, Long> {
    List<TaskChecklist> findAllByTaskOrderByDisplayOrderAscIdAsc(TaskItem task);
}
