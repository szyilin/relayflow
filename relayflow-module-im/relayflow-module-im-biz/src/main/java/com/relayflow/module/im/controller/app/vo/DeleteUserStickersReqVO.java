package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DeleteUserStickersReqVO {

    @NotEmpty
    private List<Long> ids;
}
