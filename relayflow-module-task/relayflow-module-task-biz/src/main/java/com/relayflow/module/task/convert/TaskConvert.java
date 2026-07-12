package com.relayflow.module.task.convert;

import com.relayflow.module.task.controller.app.vo.TaskItemRespVO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;

import java.util.Collections;
import java.util.List;

public final class TaskConvert {

    private TaskConvert() {
    }

    public static List<TaskItemRespVO> toRespList(List<TaskItemDO> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        return rows.stream().map(TaskConvert::toResp).toList();
    }

    public static TaskItemRespVO toResp(TaskItemDO row) {
        TaskItemRespVO vo = new TaskItemRespVO();
        vo.setId(row.getId());
        vo.setTitle(row.getTitle());
        vo.setStatus(row.getStatus());
        vo.setDueTime(row.getDueTime());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }
}
