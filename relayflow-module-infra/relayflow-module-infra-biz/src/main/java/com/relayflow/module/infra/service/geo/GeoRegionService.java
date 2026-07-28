package com.relayflow.module.infra.service.geo;

import com.relayflow.module.infra.controller.admin.geo.vo.GeoRegionNodeRespVO;
import com.relayflow.module.infra.controller.admin.geo.vo.GeoSyncMetaRespVO;

import java.util.List;

public interface GeoRegionService {

    List<GeoRegionNodeRespVO> listChildren(Long parentId);

    GeoSyncMetaRespVO getLatestSyncMeta();

    /**
     * Import / upsert China mainland four-level regions from the vendored snapshot.
     *
     * @return number of region rows touched (inserted or updated)
     */
    int syncChina();

    /**
     * If the region table is empty, run {@link #syncChina()}.
     */
    void bootstrapIfEmpty();
}
