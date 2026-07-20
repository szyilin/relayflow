package com.relayflow.module.docs.controller.app.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocPlacementMoveReqVO {

    @NotNull
    private Long objectId;

    /** DRIVE or LIBRARY */
    @NotBlank
    private String target;

    /** When target=DRIVE: destination folder (null = root). */
    private Long folderId;

    /** When target=LIBRARY: destination parent node (null = library root). */
    private Long parentId;
}
