package com.relayflow.module.infra.service.geo;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.module.infra.controller.admin.geo.vo.GeoRegionNodeRespVO;
import com.relayflow.module.infra.controller.admin.geo.vo.GeoSyncMetaRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraGeoRegionDO;
import com.relayflow.module.infra.dal.dataobject.InfraGeoSyncMetaDO;
import com.relayflow.module.infra.dal.mapper.InfraGeoRegionExtMapper;
import com.relayflow.module.infra.dal.mapper.InfraGeoRegionMapper;
import com.relayflow.module.infra.dal.mapper.InfraGeoSyncMetaMapper;
import com.relayflow.module.infra.enums.GeoRegionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoRegionServiceImpl implements GeoRegionService {

    static final String SNAPSHOT_CLASSPATH = "geo/china-level.min.json";
    static final String SOURCE = "province-city-china@8.5.8/level.min.json";
    static final String DATA_VERSION = "8.5.8";

    private final InfraGeoRegionMapper regionMapper;
    private final InfraGeoRegionExtMapper regionExtMapper;
    private final InfraGeoSyncMetaMapper syncMetaMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<GeoRegionNodeRespVO> listChildren(Long parentId) {
        Long effectiveParentId = parentId == null || parentId == 0L ? null : parentId;
        List<InfraGeoRegionDO> children = regionMapper.selectList(
                Wrappers.<InfraGeoRegionDO>lambdaQuery()
                        .eq(effectiveParentId != null, InfraGeoRegionDO::getParentId, effectiveParentId)
                        .isNull(effectiveParentId == null, InfraGeoRegionDO::getParentId)
                        .orderByAsc(InfraGeoRegionDO::getSort)
                        .orderByAsc(InfraGeoRegionDO::getCode));
        if (CollectionUtils.isEmpty(children)) {
            return List.of();
        }
        Map<Long, Long> childCounts = loadChildCounts(children.stream().map(InfraGeoRegionDO::getId).toList());
        List<GeoRegionNodeRespVO> result = new ArrayList<>(children.size());
        for (InfraGeoRegionDO region : children) {
            GeoRegionNodeRespVO vo = new GeoRegionNodeRespVO();
            vo.setId(region.getId());
            vo.setCode(region.getCode());
            vo.setName(region.getName());
            vo.setLevel(region.getLevel());
            vo.setParentId(region.getParentId());
            vo.setHasChildren(childCounts.getOrDefault(region.getId(), 0L) > 0);
            result.add(vo);
        }
        return result;
    }

    @Override
    public GeoSyncMetaRespVO getLatestSyncMeta() {
        InfraGeoSyncMetaDO meta = syncMetaMapper.selectOne(
                Wrappers.<InfraGeoSyncMetaDO>lambdaQuery()
                        .orderByDesc(InfraGeoSyncMetaDO::getSyncedAt)
                        .last("LIMIT 1"));
        if (meta == null) {
            return null;
        }
        GeoSyncMetaRespVO vo = new GeoSyncMetaRespVO();
        vo.setSource(meta.getSource());
        vo.setDataVersion(meta.getDataVersion());
        vo.setStatus(meta.getStatus());
        vo.setRegionCount(meta.getRegionCount());
        vo.setMessage(meta.getMessage());
        vo.setSyncedAt(meta.getSyncedAt());
        return vo;
    }

    @Override
    @Transactional
    public int syncChina() {
        AtomicInteger touched = new AtomicInteger();
        try (InputStream in = new ClassPathResource(SNAPSHOT_CLASSPATH).getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            if (!root.isArray()) {
                throw new IllegalStateException("China geo snapshot root must be a JSON array");
            }
            Long countryId = upsertRegion(
                    GeoRegionLevel.COUNTRY_CODE_CN, "中国大陆", GeoRegionLevel.COUNTRY, null, 0, touched);
            int provinceSort = 0;
            for (JsonNode province : root) {
                String provinceCode = text(province, "c");
                String provinceName = text(province, "n");
                if (provinceCode == null || provinceName == null) {
                    continue;
                }
                Long provinceId = upsertRegion(
                        provinceCode, provinceName, GeoRegionLevel.PROVINCE, countryId, provinceSort++, touched);
                JsonNode cities = province.get("d");
                if (cities == null || !cities.isArray() || cities.isEmpty()) {
                    continue;
                }
                boolean districtsDirect = true;
                for (JsonNode city : cities) {
                    if (city.has("d") && city.get("d").isArray() && !city.get("d").isEmpty()) {
                        districtsDirect = false;
                        break;
                    }
                }
                if (districtsDirect) {
                    String cityCode = municipalityCityCode(provinceCode);
                    Long cityId = upsertRegion(
                            cityCode, provinceName, GeoRegionLevel.CITY, provinceId, 0, touched);
                    int districtSort = 0;
                    for (JsonNode district : cities) {
                        upsertRegion(
                                text(district, "c"),
                                text(district, "n"),
                                GeoRegionLevel.DISTRICT,
                                cityId,
                                districtSort++,
                                touched);
                    }
                } else {
                    int citySort = 0;
                    for (JsonNode city : cities) {
                        Long cityId = upsertRegion(
                                text(city, "c"),
                                text(city, "n"),
                                GeoRegionLevel.CITY,
                                provinceId,
                                citySort++,
                                touched);
                        JsonNode districts = city.get("d");
                        if (districts == null || !districts.isArray()) {
                            continue;
                        }
                        int districtSort = 0;
                        for (JsonNode district : districts) {
                            upsertRegion(
                                    text(district, "c"),
                                    text(district, "n"),
                                    GeoRegionLevel.DISTRICT,
                                    cityId,
                                    districtSort++,
                                    touched);
                        }
                    }
                }
            }
            writeSyncMeta("SUCCESS", touched.get(), null);
            log.info("China geo sync success: touched={}", touched.get());
            return touched.get();
        } catch (Exception ex) {
            log.error("China geo sync failed", ex);
            writeSyncMeta("FAILED", touched.get(), ex.getMessage());
            throw new IllegalStateException("China geo sync failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void bootstrapIfEmpty() {
        Long count = regionMapper.selectCount(Wrappers.emptyWrapper());
        if (count != null && count > 0) {
            return;
        }
        log.info("infra_geo_region empty — bootstrapping China snapshot");
        syncChina();
    }

    private Map<Long, Long> loadChildCounts(List<Long> parentIds) {
        if (CollectionUtils.isEmpty(parentIds)) {
            return Map.of();
        }
        List<Map<String, Object>> rows = regionExtMapper.countChildrenByParentIds(parentIds);
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object parentId = row.get("parentid");
            if (parentId == null) {
                parentId = row.get("parentId");
            }
            Object childCount = row.get("childcount");
            if (childCount == null) {
                childCount = row.get("childCount");
            }
            if (parentId != null && childCount != null) {
                result.put(((Number) parentId).longValue(), ((Number) childCount).longValue());
            }
        }
        return result;
    }

    private Long upsertRegion(
            String code,
            String name,
            String level,
            Long parentId,
            int sort,
            AtomicInteger touched) {
        if (code == null || name == null) {
            return null;
        }
        InfraGeoRegionDO existing = regionMapper.selectOne(
                Wrappers.<InfraGeoRegionDO>lambdaQuery().eq(InfraGeoRegionDO::getCode, code).last("LIMIT 1"));
        if (existing == null) {
            InfraGeoRegionDO created = new InfraGeoRegionDO();
            created.setCode(code);
            created.setName(name);
            created.setLevel(level);
            created.setParentId(parentId);
            created.setSort(sort);
            regionMapper.insert(created);
            touched.incrementAndGet();
            return created.getId();
        }
        boolean dirty = !name.equals(existing.getName())
                || !level.equals(existing.getLevel())
                || (parentId == null ? existing.getParentId() != null : !parentId.equals(existing.getParentId()))
                || existing.getSort() == null
                || existing.getSort() != sort;
        if (dirty) {
            existing.setName(name);
            existing.setLevel(level);
            existing.setParentId(parentId);
            existing.setSort(sort);
            regionMapper.updateById(existing);
            touched.incrementAndGet();
        }
        return existing.getId();
    }

    private void writeSyncMeta(String status, int regionCount, String message) {
        InfraGeoSyncMetaDO meta = new InfraGeoSyncMetaDO();
        meta.setSource(SOURCE);
        meta.setDataVersion(DATA_VERSION);
        meta.setStatus(status);
        meta.setRegionCount(regionCount);
        meta.setMessage(message != null && message.length() > 500 ? message.substring(0, 500) : message);
        meta.setSyncedAt(OffsetDateTime.now());
        syncMetaMapper.insert(meta);
    }

    private static String municipalityCityCode(String provinceCode) {
        if (provinceCode != null && provinceCode.length() >= 2) {
            return provinceCode.substring(0, 2) + "0100";
        }
        return provinceCode + "-city";
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
