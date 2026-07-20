package com.relayflow.module.docs.controller.app.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public class DocDriveItemUpdateReqVO {

    private String title;

    @Setter(AccessLevel.NONE)
    private Long folderId;

    private Integer sortOrder;

    private boolean folderIdSpecified;

    @JsonProperty("folderId")
    public void setFolderIdValue(Long folderId) {
        this.folderId = folderId;
        this.folderIdSpecified = true;
    }
}
