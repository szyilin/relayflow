package com.relayflow.module.im.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateGroupReqVO {

    @NotBlank
    @Size(min = 1, max = 128)
    private String name;

    @NotEmpty
    private List<Long> memberUserIds;
}
