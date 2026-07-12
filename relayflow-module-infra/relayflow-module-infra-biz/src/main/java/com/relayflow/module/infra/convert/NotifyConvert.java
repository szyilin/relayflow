package com.relayflow.module.infra.convert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.module.infra.controller.app.notify.vo.NotifyItemRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class NotifyConvert {

    private NotifyConvert() {
    }

    public static List<NotifyItemRespVO> toRespList(List<InfraNotifyDO> rows, ObjectMapper objectMapper) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(row -> toResp(row, objectMapper)).toList();
    }

    public static NotifyItemRespVO toResp(InfraNotifyDO row, ObjectMapper objectMapper) {
        NotifyItemRespVO vo = new NotifyItemRespVO();
        vo.setId(row.getId());
        vo.setTenantId(row.getTenantId());
        vo.setType(row.getType());
        vo.setTitle(row.getTitle());
        vo.setBody(row.getBody());
        vo.setPayload(parsePayload(row.getPayloadJson(), objectMapper));
        vo.setRead(row.getReadFlag() != null && row.getReadFlag() == 1);
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parsePayload(Object payloadJson, ObjectMapper objectMapper) {
        if (payloadJson == null) {
            return null;
        }
        if (payloadJson instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
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
