package com.relayflow.module.docs.controller.admin.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DocStorageQuotaRespVO {

    /** null = unlimited for all members without override */
    private Long defaultQuotaBytes;

    @Valid
    @NotNull
    private List<DocStorageQuotaUserRuleRespVO> userRules = new ArrayList<>();

    @Data
    public static class DocStorageQuotaUserRuleRespVO {
        @NotNull
        private Long userId;
        private String displayName;
        /** null = unlimited for this user */
        private Long quotaBytes;
    }
}
