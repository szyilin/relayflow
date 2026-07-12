package com.relayflow.module.infra.controller.app.notify.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class NotifyReadReqVO {

    @NotEmpty(message = "通知 id 列表不能为空")
    private List<Long> ids;
}
