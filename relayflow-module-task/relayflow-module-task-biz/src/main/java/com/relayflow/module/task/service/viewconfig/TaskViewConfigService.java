package com.relayflow.module.task.service.viewconfig;

import com.relayflow.module.task.controller.app.vo.TaskViewConfigSaveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskViewConfigVO;

public interface TaskViewConfigService {

    TaskViewConfigVO get(String contextType, Long contextId);

    void save(TaskViewConfigSaveReqVO request);
}
