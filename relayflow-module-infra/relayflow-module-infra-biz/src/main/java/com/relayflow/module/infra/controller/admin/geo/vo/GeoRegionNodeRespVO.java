package com.relayflow.module.infra.controller.admin.geo.vo;

import lombok.Data;

@Data
public class GeoRegionNodeRespVO {

    private Long id;
    private String code;
    private String name;
    private String level;
    private Long parentId;
    private Boolean hasChildren;
}
