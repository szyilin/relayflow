package com.relayflow.module.task.controller.app.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class TaskListFieldCreateReqVO {

    @NotNull
    private Long listId;

    @NotBlank
    private String name;

    /** V1: SINGLE_SELECT only */
    private String fieldType;

    @NotEmpty
    @Valid
    private List<OptionDraft> options;

    @Data
    public static class OptionDraft {
        @NotBlank
        private String label;
        private String valueKey;
    }
}
