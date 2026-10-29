package com.relayflow.module.infra.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.context.XxlJobHelper;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.infra.api.file.*;
import com.relayflow.module.infra.api.file.dto.FileBindReqDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Explicit operator task; defaults to dry-run and processes exactly one resumable page. */
@Component
@RequiredArgsConstructor
public class PrivateFileBindingRepairJob {
    private final ObjectProvider<FileHistorySource> sources;
    private final FileApi files;
    private final ObjectMapper json;
    @Data public static class Plan {
        private Long tenantId;
        private String bizType;
        private long afterId;
        private int limit = 100;
        private boolean execute;
    }
    public record Report(long nextId, boolean done, int checked, int eligible, int rejected) {}
    @XxlJob("privateFileBindingRepair")
    public void run() throws Exception {
        Plan plan = json.readValue(XxlJobHelper.getJobParam(), Plan.class);
        Report report = repair(plan);
        XxlJobHelper.log("nextId={}, done={}, checked={}, eligible={}, rejected={}", report.nextId(), report.done(),
                report.checked(), report.eligible(), report.rejected());
    }
    public Report repair(Plan plan) {
        if (plan.getTenantId() == null || plan.getTenantId() <= 0 || plan.getLimit() < 1 || plan.getLimit() > 500 || plan.getAfterId() < 0)
            throw new IllegalArgumentException("Explicit tenant and bounded page required");
        var source = sources.orderedStream().filter(s -> s.bizType().equals(plan.getBizType())).findFirst().orElseThrow();
        Long previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(plan.getTenantId());
            var page = source.scan(plan.getTenantId(), plan.getAfterId(), plan.getLimit());
            int eligible = 0, rejected = 0;
            for (var ref : page.references()) {
                try {
                    if (!source.trusted(plan.getTenantId(), ref)) throw new IllegalArgumentException();
                    if (plan.isExecute()) {
                        var binding = new FileBindReqDTO(); binding.setFileId(ref.fileId());
                        binding.setBizId(ref.bizId()); binding.setBizType(source.bizType()); files.bindFile(binding);
                    }
                    eligible++;
                } catch (RuntimeException ex) {
                    rejected++;
                    XxlJobHelper.log("Rejected reference: bizId={}, fileId={}", ref.bizId(), ref.fileId());
                }
            }
            return new Report(page.nextId(), page.done(), page.references().size(), eligible, rejected);
        } finally {
            if (previous == null) TenantContextHolder.clear(); else TenantContextHolder.set(previous);
        }
    }
}
