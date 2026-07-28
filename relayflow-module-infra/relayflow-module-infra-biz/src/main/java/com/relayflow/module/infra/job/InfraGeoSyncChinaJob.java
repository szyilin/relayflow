package com.relayflow.module.infra.job;

import com.relayflow.module.infra.service.geo.GeoRegionService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Monthly China admin-division sync. Seed Cron: {@code 0 0 3 1 * ?} in deploy/xxl-job/seed.
 */
@Component
@RequiredArgsConstructor
public class InfraGeoSyncChinaJob {

    private final GeoRegionService geoRegionService;

    @XxlJob("infraGeoSyncChina")
    public void execute() {
        int touched = geoRegionService.syncChina();
        XxlJobHelper.log("infraGeoSyncChina finished, touched=" + touched);
        XxlJobHelper.handleSuccess("touched=" + touched);
    }
}
