package com.relayflow.module.infra.dal.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Mapper
public interface InfraGeoRegionExtMapper {

    /**
     * @return rows of {@code parent_id} -> {@code child_count}
     */
    List<Map<String, Object>> countChildrenByParentIds(@Param("parentIds") Collection<Long> parentIds);
}
