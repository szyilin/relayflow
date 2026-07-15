package com.relayflow.module.infra.convert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.module.infra.controller.app.notify.vo.NotifyItemRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper
public interface NotifyConvert {

    NotifyConvert INSTANCE = Mappers.getMapper(NotifyConvert.class);

    default List<NotifyItemRespVO> toRespList(List<InfraNotifyDO> rows, @Context ObjectMapper objectMapper) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(row -> toResp(row, objectMapper)).toList();
    }

    @Mapping(target = "read", expression = "java(row.getReadFlag() != null && row.getReadFlag() == 1)")
    @Mapping(target = "payload", source = "row", qualifiedByName = "parsePayload")
    NotifyItemRespVO toResp(InfraNotifyDO row, @Context ObjectMapper objectMapper);

    @Named("parsePayload")
    default Map<String, Object> parsePayload(InfraNotifyDO row, @Context ObjectMapper objectMapper) {
        Object payloadJson = row.getPayloadJson();
        if (payloadJson == null) {
            return null;
        }
        if (payloadJson instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        }
        if (payloadJson instanceof String json && !json.isBlank()) {
            try {
                return objectMapper.readValue(json, new TypeReference<>() {
                });
            } catch (Exception ignored) {
                return Collections.emptyMap();
            }
        }
        return null;
    }
}
