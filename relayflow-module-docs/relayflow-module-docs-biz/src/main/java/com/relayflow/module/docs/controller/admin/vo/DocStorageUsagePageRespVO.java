package com.relayflow.module.docs.controller.admin.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DocStorageUsagePageRespVO {

    private List<DocStorageUsageItemRespVO> list = new ArrayList<>();
    private int total;
    private int pageNo;
    private int pageSize;

    @Data
    public static class DocStorageUsageItemRespVO {
        private Long userId;
        private String displayName;
        private Long usedBytes;
        /** null = unlimited */
        private Long quotaBytes;
        /** null when unlimited; otherwise used/quota clamped to [0,1+] */
        private Double usageRatio;
    }
}
