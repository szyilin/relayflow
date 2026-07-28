package com.relayflow.module.infra.api.geo;

import com.relayflow.module.infra.api.geo.dto.GeoRegionDTO;
import com.relayflow.module.infra.controller.admin.geo.vo.GeoRegionNodeRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraGeoRegionDO;
import com.relayflow.module.infra.dal.mapper.InfraGeoRegionMapper;
import com.relayflow.module.infra.service.geo.GeoRegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GeoRegionApiImpl implements GeoRegionApi {

    private final GeoRegionService geoRegionService;
    private final InfraGeoRegionMapper regionMapper;

    @Override
    public List<GeoRegionDTO> listChildren(Long parentId) {
        return geoRegionService.listChildren(parentId).stream().map(this::toDto).toList();
    }

    @Override
    public GeoRegionDTO getById(Long id) {
        if (id == null) {
            return null;
        }
        InfraGeoRegionDO region = regionMapper.selectById(id);
        if (region == null) {
            return null;
        }
        GeoRegionDTO dto = new GeoRegionDTO();
        dto.setId(region.getId());
        dto.setCode(region.getCode());
        dto.setName(region.getName());
        dto.setLevel(region.getLevel());
        dto.setParentId(region.getParentId());
        dto.setHasChildren(null);
        return dto;
    }

    @Override
    public String buildPath(Long regionId) {
        if (regionId == null) {
            return "";
        }
        List<String> names = new ArrayList<>();
        Long cursor = regionId;
        int guard = 0;
        while (cursor != null && guard++ < 8) {
            InfraGeoRegionDO region = regionMapper.selectById(cursor);
            if (region == null) {
                break;
            }
            names.add(region.getName());
            cursor = region.getParentId();
        }
        Collections.reverse(names);
        return String.join("/", names);
    }

    private GeoRegionDTO toDto(GeoRegionNodeRespVO vo) {
        GeoRegionDTO dto = new GeoRegionDTO();
        dto.setId(vo.getId());
        dto.setCode(vo.getCode());
        dto.setName(vo.getName());
        dto.setLevel(vo.getLevel());
        dto.setParentId(vo.getParentId());
        dto.setHasChildren(vo.getHasChildren());
        return dto;
    }
}
