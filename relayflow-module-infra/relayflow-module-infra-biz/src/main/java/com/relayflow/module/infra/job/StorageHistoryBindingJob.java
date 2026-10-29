package com.relayflow.module.infra.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import com.relayflow.module.infra.dal.mapper.StorageHistoryExtMapper;
import com.relayflow.module.infra.dal.mapper.StorageRevisionExtMapper;
import com.relayflow.module.infra.service.storage.StorageLocationService;
import com.relayflow.framework.oss.core.ObjectStorageClientFactory;
import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;

/** Explicit operator plan, one page per execution. Never guesses a source from a successful HEAD. */
@Component
@RequiredArgsConstructor
public class StorageHistoryBindingJob {
    private final ObjectMapper json;
    private final StorageHistoryExtMapper history;
    private final StorageRevisionExtMapper revisions;
    private final StorageLocationService locations;
    private final ObjectStorageClientFactory clients;
    @Data public static class Plan {
        private Long tenantId;
        private Long revisionId;
        private String source;
        private String bucket;
        private OffsetDateTime from;
        private OffsetDateTime to;
        private long afterId;
        private int limit=100;
        private boolean execute;
        private String confirmPlan;
    }
    public record Report(String planHash,long nextId,boolean done,int candidates,int verified,int bound,int unresolved,int rejected) {}
    @XxlJob("storageHistoryBinding")
    public void run() throws Exception {
        var report=process(json.readValue(XxlJobHelper.getJobParam(),Plan.class));
        XxlJobHelper.log("plan={}, nextId={}, done={}, candidates={}, verified={}, bound={}, unresolved={}, rejected={}",
                report.planHash(),report.nextId(),report.done(),report.candidates(),report.verified(),report.bound(),report.unresolved(),report.rejected());
    }
    public Report process(Plan plan) {
        if(plan.getTenantId()==null||plan.getTenantId()<=0||plan.getBucket()==null||plan.getBucket().isBlank()
                ||plan.getFrom()==null||plan.getTo()==null||plan.getFrom().isAfter(plan.getTo())
                ||plan.getLimit()<1||plan.getLimit()>500||plan.getAfterId()<0||!("bootstrap".equals(plan.getSource())||"tenant".equals(plan.getSource())))
            throw new IllegalArgumentException("Explicit tenant/source/bucket/time range and bounded page required");
        String hash=StorageLocationService.hash(plan.getTenantId()+"|"+plan.getRevisionId()+"|"+plan.getSource()+"|"+plan.getBucket()+"|"+plan.getFrom().toInstant()+"|"+plan.getTo().toInstant());
        if(plan.isExecute() && (plan.getRevisionId()==null||!hash.equals(plan.getConfirmPlan())))
            throw new IllegalArgumentException("Explicit confirmed dry-run plan required");
        var target=plan.getRevisionId()==null?null:locations.require(plan.getTenantId(),plan.getRevisionId());
        if(target!=null && (!target.getSource().equals(plan.getSource()) || !locations.parse(target.getConfigJson()).getBucket().equals(plan.getBucket())))
            throw new IllegalArgumentException("Target must match confirmed source and bucket");
        var rows=history.page(plan.getTenantId(),"minio://"+plan.getBucket()+"/",plan.getFrom(),plan.getTo(),plan.getAfterId(),plan.getLimit());
        int verified=0,bound=0,unresolved=0,rejected=0;
        for(var file:rows) {
            if(target==null){unresolved++;continue;}
            try {
                var config=locations.resolve(plan.getTenantId(),target.getId());
                var meta=clients.getClient(ObjectStorageProviderType.MINIO).headObject(config,file.getObjectKey());
                if(meta==null||file.getSize()==null||file.getSize()!=meta.getSize()) throw new IllegalArgumentException();
                verified++;
                if(plan.isExecute())bound+=revisions.bindFile(plan.getTenantId(),file.getId(),target.getId());
            }catch(RuntimeException ex){rejected++;XxlJobHelper.log("Unverified fileId={}",file.getId());}
        }
        return new Report(hash,rows.isEmpty()?plan.getAfterId():rows.getLast().getId(),rows.size()<plan.getLimit(),rows.size(),verified,bound,unresolved,rejected);
    }
}
