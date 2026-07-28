package com.relayflow.module.infra.api.geo.dto;

import lombok.Data;

@Data
public class GeoRegionDTO {

    private Long id;
    private String code;
    private String name;
    private String level;
    private Long parentId;
    private Boolean hasChildren;
}
