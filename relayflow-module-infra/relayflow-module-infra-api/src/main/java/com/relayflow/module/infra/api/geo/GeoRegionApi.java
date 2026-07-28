package com.relayflow.module.infra.api.geo;

import com.relayflow.module.infra.api.geo.dto.GeoRegionDTO;

import java.util.List;

/**
 * Cross-domain read API for China geo cascade (system address forms, etc.).
 */
public interface GeoRegionApi {

    /**
     * @param parentId null or 0 for root nodes
     */
    List<GeoRegionDTO> listChildren(Long parentId);

    GeoRegionDTO getById(Long id);

    /**
     * Join display path like {@code 中国大陆/安徽省/合肥市/瑶海区}.
     */
    String buildPath(Long regionId);
}
