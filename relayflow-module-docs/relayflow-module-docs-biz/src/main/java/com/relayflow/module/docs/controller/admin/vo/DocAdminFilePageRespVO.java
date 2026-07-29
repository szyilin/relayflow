package com.relayflow.module.docs.controller.admin.vo;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DocAdminFilePageRespVO {

    private List<DocAdminFileItemRespVO> list = new ArrayList<>();
    private int total;
    private int pageNo;
    private int pageSize;

    @Data
    public static class DocAdminFileItemRespVO {
        private Long objectId;
        private String title;
        private String type;
        private String lifecycle;
        private Long ownerUserId;
        private String ownerDisplayName;
        private OffsetDateTime updatedAt;
        /** FILE only; null for RICH_DOC */
        private Long sizeBytes;
        private OffsetDateTime deletedAt;
        /** Remaining days in restore window; null when ACTIVE */
        private Integer daysUntilPurge;
    }
}
