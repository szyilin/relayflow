package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DocLibraryNodeTreeVO {

    private Long nodeId;

    private Long parentId;

    private Long objectId;

    private String title;

    private Integer sortOrder;

    private List<DocLibraryNodeTreeVO> children = new ArrayList<>();
}
