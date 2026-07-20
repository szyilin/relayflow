package com.relayflow.module.docs.controller.app.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class DocDriveFolderUpdateReqVO {

    private String name;

    @Setter(AccessLevel.NONE)
    private Long parentId;

    private Integer sortOrder;

    private boolean parentIdSpecified;

    @JsonProperty("parentId")
    public void setParentIdValue(Long parentId) {
        this.parentId = parentId;
        this.parentIdSpecified = true;
    }
}
