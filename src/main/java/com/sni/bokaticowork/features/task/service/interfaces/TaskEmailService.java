package com.sni.bokaticowork.features.task.service.interfaces;

import com.sni.bokaticowork.features.task.model.TaskItem;

public interface TaskEmailService {
    void sendTaskAssigned(TaskItem task);
    void sendTaskDueSoon(TaskItem task);
    void sendTaskOverdue(TaskItem task);
}
