package com.relayflow.module.task.controller.app.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskItemUpdateReqVO {

    @NotNull
    private Long id;

    @Size(max = 200)
    private String title;

    private OffsetDateTime startTime;

    private OffsetDateTime dueTime;

    private Integer remindBeforeMinutes;

    private String description;

    @JsonIgnore
    private boolean titlePresent;

    @JsonIgnore
    private boolean startTimePresent;

    @JsonIgnore
    private boolean dueTimePresent;

    @JsonIgnore
    private boolean remindBeforeMinutesPresent;

    @JsonIgnore
    private boolean descriptionPresent;

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = title;
        this.titlePresent = true;
    }

    @JsonSetter("startTime")
    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
        this.startTimePresent = true;
    }

    @JsonSetter("dueTime")
    public void setDueTime(OffsetDateTime dueTime) {
        this.dueTime = dueTime;
        this.dueTimePresent = true;
    }

    @JsonSetter("remindBeforeMinutes")
    public void setRemindBeforeMinutes(Integer remindBeforeMinutes) {
        this.remindBeforeMinutes = remindBeforeMinutes;
        this.remindBeforeMinutesPresent = true;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
        this.descriptionPresent = true;
    }
}
