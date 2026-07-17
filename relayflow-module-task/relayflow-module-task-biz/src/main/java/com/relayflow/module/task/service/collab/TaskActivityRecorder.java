package com.relayflow.module.task.service.collab;

import com.relayflow.module.task.dal.dataobject.TaskItemDO;

public interface TaskActivityRecorder {

    void record(TaskItemDO task, Long actorId, String type, String summary);
}
