package com.relayflow.module.infra.service.storage;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.encrypt.AesGcmEncryptor;
import com.relayflow.framework.oss.config.StorageProperties;
import com.relayflow.framework.oss.core.*;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import com.relayflow.module.infra.dal.mapper.*;
import com.relayflow.module.infra.job.StorageHistoryBindingJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Destructive writes are confined to a restored COPY; objects are only HEAD/GET. */
@EnabledIfEnvironmentVariable(named="RELAYFLOW_STORAGE_COPY_JDBC_URL",matches="jdbc:postgresql:.*storage_upgrade_copy.*")
class StorageUpgradeCopyPostgresTest {
    @Test void confirmedMappingSurvivesInterruptedBatchAndPreservesEveryOriginalKeyUriAndByte() throws Exception {
        long tenant=Long.parseLong(System.getenv("RELAYFLOW_STORAGE_HISTORY_TENANT"));
        var ds=new DriverManagerDataSource(System.getenv("RELAYFLOW_STORAGE_COPY_JDBC_URL"),System.getenv("RELAYFLOW_AUTH_TEST_JDBC_USER"),System.getenv("RELAYFLOW_AUTH_TEST_JDBC_PASSWORD"));
        var cfg=new MybatisConfiguration();cfg.setMapUnderscoreToCamelCase(true);cfg.addMapper(StorageRevisionExtMapper.class);cfg.addMapper(StorageHistoryExtMapper.class);
        var bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(cfg);
        var json=new ObjectMapper().findAndRegisterModules();var factory=new ObjectStorageClientFactory();
        var properties=new StorageProperties();properties.getMinio().setEndpoint("http://localhost:19000");
        // Explicitly confirmed source: private-file-access isolated fixture, original Bootstrap bucket, no prefix.
        var original=StorageProviderConfig.builder().providerType(ObjectStorageProviderType.MINIO).endpoint("http://localhost:19000").bucket("relayflow").accessKey("relayflow_test").secretKey("relayflow_test_only").pathPrefix("").build();
        record Before(long id,String key,String uri,long size,byte[] bytes){}
        var before=new ArrayList<Before>();
        try(var c=ds.getConnection();var st=c.prepareStatement("SELECT id,object_key,storage_uri,size FROM infra_file WHERE tenant_id=? AND storage_config_revision_id IS NULL ORDER BY id")) {
            st.setLong(1,tenant);try(var rs=st.executeQuery()){while(rs.next())before.add(new Before(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4),read(factory,original,rs.getString(2))));}
        }
        assertTrue(before.size()>=3,"Use the backed-up private-file fixture tenant, never a live database");
        try(var session=bean.getObject().openSession(true)) {
            var revisions=session.getMapper(StorageRevisionExtMapper.class);var history=session.getMapper(StorageHistoryExtMapper.class);
            var locations=new StorageLocationService(revisions,properties,new AesGcmEncryptor(new byte[32]),json,factory);
            var target=locations.ensure(tenant,"bootstrap",original,true);
            var plan=new StorageHistoryBindingJob.Plan();plan.setTenantId(tenant);plan.setRevisionId(target.getId());plan.setSource("bootstrap");plan.setBucket("relayflow");plan.setFrom(OffsetDateTime.parse("2026-10-29T00:00:00+08:00"));plan.setTo(OffsetDateTime.parse("2026-10-30T00:00:00+08:00"));plan.setLimit(1);
            var job=new StorageHistoryBindingJob(json,history,revisions,locations,factory);
            var preview=job.process(plan);assertEquals(1,preview.verified());assertEquals(before.size(),revisions.unresolvedFiles(tenant));
            plan.setExecute(true);plan.setConfirmPlan(preview.planHash());
            var first=job.process(plan);assertEquals(1,first.bound());plan.setAfterId(first.nextId());
            // Simulate interrupted worker: reconstruct it and resume with the same fixed plan hash/cursor.
            job=new StorageHistoryBindingJob(json,history,revisions,locations,factory);int bound=first.bound();
            while(true){var page=job.process(plan);assertEquals(0,page.rejected());bound+=page.bound();plan.setAfterId(page.nextId());if(page.done())break;}
            assertEquals(before.size(),bound);assertEquals(0,revisions.unresolvedFiles(tenant));plan.setAfterId(0);assertEquals(0,job.process(plan).bound());
            for(var row:before) {
                try(var c=ds.getConnection();var st=c.prepareStatement("SELECT object_key,storage_uri,size,storage_config_revision_id FROM infra_file WHERE tenant_id=? AND id=?")) {
                    st.setLong(1,tenant);st.setLong(2,row.id());try(var rs=st.executeQuery()){assertTrue(rs.next());assertEquals(row.key(),rs.getString(1));assertEquals(row.uri(),rs.getString(2));assertEquals(row.size(),rs.getLong(3));assertEquals(target.getId().longValue(),rs.getLong(4));}
                }
                assertArrayEquals(row.bytes(),read(factory,locations.resolve(tenant,target.getId()),row.key()));
            }
            System.out.println("storage upgrade copy: bound="+bound+", unresolved=0, rejected=0, replay=0; all original keys/URIs/sizes/bytes unchanged");
        }
    }
    private byte[] read(ObjectStorageClientFactory factory,StorageProviderConfig config,String key)throws Exception {
        var uri=factory.getClient(ObjectStorageProviderType.MINIO).createPresignedGet(config,key,Duration.ofMinutes(1));
        var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(uri)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());assertEquals(200,response.statusCode());return response.body();
    }
}
