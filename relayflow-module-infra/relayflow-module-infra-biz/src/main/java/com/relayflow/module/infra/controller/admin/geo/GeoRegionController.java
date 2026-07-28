package com.relayflow.module.infra.controller.admin.geo;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.infra.controller.admin.geo.vo.GeoRegionNodeRespVO;
import com.relayflow.module.infra.controller.admin.geo.vo.GeoSyncMetaRespVO;
import com.relayflow.module.infra.service.geo.GeoRegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin-api/infra/geo")
@RequiredArgsConstructor
public class GeoRegionController {

    private final GeoRegionService geoRegionService;

    @GetMapping("/children")
    @PreAuthorize("hasAuthority('infra:geo:query')")
    public CommonResult<List<GeoRegionNodeRespVO>> children(
            @RequestParam(value = "parentId", required = false) Long parentId) {
        return CommonResult.success(geoRegionService.listChildren(parentId));
    }

    @GetMapping("/sync-meta")
    @PreAuthorize("hasAuthority('infra:geo:query')")
    public CommonResult<GeoSyncMetaRespVO> syncMeta() {
        return CommonResult.success(geoRegionService.getLatestSyncMeta());
    }

    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('infra:geo:sync')")
    public CommonResult<Map<String, Object>> sync() {
        int touched = geoRegionService.syncChina();
        return CommonResult.success(Map.of("touched", touched));
    }
}
