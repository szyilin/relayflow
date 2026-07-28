package com.relayflow.module.infra.config;

import com.relayflow.module.infra.service.geo.GeoRegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ensure China geo cascade data exists after empty DB / fresh migrate.
 */
@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class GeoBootstrapRunner implements ApplicationRunner {

    private final GeoRegionService geoRegionService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            geoRegionService.bootstrapIfEmpty();
        } catch (Exception ex) {
            log.warn("Geo bootstrap skipped/failed: {}", ex.getMessage());
        }
    }
}
