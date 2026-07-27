package com.relayflow.module.system.controller.app.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AppContactStarredUpdateReqVO {

    @NotNull
    private Boolean starred;
}
