package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

@Data
public class DocPlacementMoveRespVO {

    private Long objectId;
    private String target;
    /** Drive itemId or Library nodeId after move. */
    private Long placementId;
}
