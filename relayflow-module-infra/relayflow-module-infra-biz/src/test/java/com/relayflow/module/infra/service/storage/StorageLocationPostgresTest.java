package com.relayflow.module.infra.service.storage;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.encrypt.AesGcmEncryptor;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.oss.config.StorageProperties;
import com.relayflow.framework.oss.core.*;
import com.relayflow.framework.oss.core.model.*;
import com.relayflow.module.infra.dal.mapper.*;
import com.relayflow.module.infra.job.StorageHistoryBindingJob;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@EnabledIfEnvironmentVariable(named="RELAYFLOW_AUTH_TEST_JDBC_URL",matches="jdbc:postgresql:.*")
class StorageLocationPostgresTest {
    private DriverManagerDataSource ds;
    private SqlSession session;
    private StorageLocationService service;
    private StorageRevisionExtMapper revisions;
    private StorageHistoryExtMapper history;
    private StorageProperties properties;
    private ObjectStorageClient client;
    private ObjectStorageClientFactory clients;
    private final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
    private final long tenant=887101;
    @BeforeEach void setup() throws Exception {
        ds=new DriverManagerDataSource(System.getenv("RELAYFLOW_AUTH_TEST_JDBC_URL"),System.getenv("RELAYFLOW_AUTH_TEST_JDBC_USER"),System.getenv("RELAYFLOW_AUTH_TEST_JDBC_PASSWORD"));
        clean();
        var cfg=new MybatisConfiguration();cfg.setMapUnderscoreToCamelCase(true);cfg.addMapper(StorageRevisionExtMapper.class);cfg.addMapper(StorageHistoryExtMapper.class);
        cfg.addMapper(InfraFileMapper.class);cfg.addMapper(InfraFileUploadSessionMapper.class);
        var bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(cfg);session=bean.getObject().openSession(true);
        revisions=session.getMapper(StorageRevisionExtMapper.class);history=session.getMapper(StorageHistoryExtMapper.class);
        properties=MinioAddressingTest.managedProperties("http://managed:9000","http://localhost:19000");
        client=mock(ObjectStorageClient.class);clients=mock(ObjectStorageClientFactory.class);lenient().when(clients.getClient(ObjectStorageProviderType.MINIO)).thenReturn(client);
        service=new StorageLocationService(revisions,properties,new AesGcmEncryptor(new byte[32]),json,clients);
    }
    @AfterEach void cleanup()throws Exception{if(session!=null)session.close();if(ds!=null)clean();}
    private void clean()throws Exception{execute("DELETE FROM infra_file_upload_session WHERE tenant_id=887101");execute("DELETE FROM infra_file WHERE tenant_id=887101");execute("DELETE FROM infra_storage_provider WHERE tenant_id=887101");execute("DELETE FROM infra_storage_config_revision WHERE tenant_id=887101");}
    private StorageProviderConfig config(String bucket,String prefix){return StorageProviderConfig.builder().providerType(ObjectStorageProviderType.MINIO).endpoint("http://managed:9000").region("us-east-1").bucket(bucket).accessKey("access-test").secretKey("secret-test").pathPrefix(prefix).build();}
    @Test void locationIsStableAcrossPublicPortChangeAndRetainsSelectedCredentialsRegionAndPrefix() {
        var revision=service.ensure(tenant,"bootstrap",config("a","old/"),false);
        var same=service.ensure(tenant,"bootstrap",config("a","old/"),false);assertEquals(revision.getId(),same.getId());
        properties.getMinio().setPublicEndpoint("http://localhost:29000");
        var runtime=service.resolve(tenant,revision.getId());assertEquals("http://localhost:29000",runtime.getPublicEndpoint());assertEquals("a",runtime.getBucket());assertEquals("us-east-1",runtime.getRegion());assertEquals("",runtime.getPathPrefix());
        assertEquals("old/",service.pin(tenant,revision.getId()).pathPrefix());
        assertFalse(revision.getConfigJson().contains("secret-test"));assertFalse(runtime.toString().contains("secret-test"));
        assertThrows(ServiceException.class,()->service.resolve(tenant+1,revision.getId()));assertThrows(ServiceException.class,()->service.resolve(tenant,null));
        var changed=service.ensure(tenant,"bootstrap",config("b","new/"),false);assertNotEquals(revision.getId(),changed.getId());
    }
    @Test void independentEndpointIsNeverReplacedByDeploymentRoute() {
        var independent=config("independent","").toBuilder().endpoint("https://independent.example").region("region-test").build();
        var revision=service.ensure(tenant,"tenant",independent,false);properties.getMinio().setEndpoint("http://different:9000");
        var runtime=service.resolve(tenant,revision.getId());assertEquals("https://independent.example:443",runtime.getEndpoint());assertNull(runtime.getPublicEndpoint());assertEquals("region-test",runtime.getRegion());
    }
    @Test void explicitRotationValidatesBeforeWriteAndFailurePreservesOldCredentials() {
        var revision=service.ensure(tenant,"tenant",config("a","prefix/"),false);
        var newCredentials=config("a","prefix/").toBuilder().secretKey("new-secret").build();
        assertEquals(2002001007,assertThrows(ServiceException.class,()->service.ensure(tenant,"tenant",newCredentials,false)).getCode());
        doThrow(new IllegalStateException("invalid credentials")).when(client).checkConnectivity(any());
        assertThrows(ServiceException.class,()->service.rotate(tenant,revision.getId(),"new-access","new-secret"));assertEquals("secret-test",service.resolve(tenant,revision.getId()).getSecretKey());
        doNothing().when(client).checkConnectivity(any());service.rotate(tenant,revision.getId(),"new-access","new-secret");
        assertEquals("new-secret",service.resolve(tenant,revision.getId()).getSecretKey());assertEquals("a",service.resolve(tenant,revision.getId()).getBucket());assertEquals(revision.getLocationKey(),service.require(tenant,revision.getId()).getLocationKey());
    }
    @Test void fileAndUnexpiredSessionReferencesIncludeRetainedDeletedFilesAndIgnoreExpiredSessions()throws Exception {
        var revision=service.ensure(tenant,"tenant",config("a",""),false);
        execute("INSERT INTO infra_file(id,tenant_id,provider,storage_uri,object_key,original_name,size,deleted,storage_config_revision_id) VALUES(887201,887101,'minio','minio://a/key','key','old',10,1,"+revision.getId()+")");
        assertTrue(service.referenced(tenant,revision.getId()));execute("DELETE FROM infra_file WHERE id=887201");
        execute("INSERT INTO infra_file_upload_session(id,tenant_id,provider,object_key,original_name,status,expires_at,storage_config_revision_id) VALUES(887202,887101,'minio','key','pending','pending',NOW()+INTERVAL '1 minute',"+revision.getId()+")");
        assertTrue(service.referenced(tenant,revision.getId()));execute("UPDATE infra_file_upload_session SET expires_at=NOW()-INTERVAL '1 minute' WHERE id=887202");assertFalse(service.referenced(tenant,revision.getId()));
    }
    @Test void historyDryRunRequiresExplicitMappingAndResumeNeverOverwritesExistingBindingOrKey()throws Exception {
        var revision=service.ensure(tenant,"bootstrap",config("legacy","old/"),true);
        assertThrows(ServiceException.class,()->service.pin(tenant,revision.getId()));
        assertEquals("old/",service.resolve(tenant,revision.getId()).getPathPrefix());
        execute("INSERT INTO infra_file(id,tenant_id,provider,storage_uri,object_key,original_name,size) VALUES(887301,887101,'minio','minio://legacy/one','one','one',10),(887302,887101,'minio','minio://legacy/two','two','two',10)");
        when(client.headObject(any(),any())).thenReturn(StorageObjectMeta.builder().size(10).build());
        var job=new StorageHistoryBindingJob(json,history,revisions,service,clients);var plan=new StorageHistoryBindingJob.Plan();plan.setTenantId(tenant);plan.setSource("bootstrap");plan.setBucket("legacy");plan.setFrom(OffsetDateTime.now().minusDays(1));plan.setTo(OffsetDateTime.now().plusHours(1));plan.setLimit(1);
        var unknown=job.process(plan);assertEquals(1,unknown.unresolved());assertEquals(2,revisions.unresolvedFiles(tenant));
        plan.setExecute(true);assertThrows(IllegalArgumentException.class,()->job.process(plan));plan.setExecute(false);plan.setRevisionId(revision.getId());
        var preview=job.process(plan);assertEquals(1,preview.verified());assertEquals(2,revisions.unresolvedFiles(tenant));
        plan.setExecute(true);plan.setConfirmPlan(preview.planHash());var first=job.process(plan);assertEquals(1,first.bound());
        plan.setAfterId(first.nextId());var second=job.process(plan);assertEquals(1,second.bound());assertEquals(0,revisions.unresolvedFiles(tenant));
        plan.setAfterId(0);assertEquals(0,job.process(plan).bound());
        try(Connection c=ds.getConnection();var st=c.createStatement();var rs=st.executeQuery("SELECT object_key FROM infra_file WHERE id=887301")){assertTrue(rs.next());assertEquals("one",rs.getString(1));}
        var wrong=service.ensure(tenant,"bootstrap",config("different",""),false);assertEquals(0,revisions.bindFile(tenant,887301L,wrong.getId()));
    }
    @Test void bootstrapPinKeepsExplicitlyRotatedCredentialsInsteadOfOverwritingFromEnvironment() {
        var revision=service.ensureBootstrap(tenant,config("a",""));service.rotate(tenant,revision.getId(),"new-access","new-secret");
        var again=service.ensureBootstrap(tenant,config("a",""));assertEquals(revision.getId(),again.getId());assertEquals("new-secret",service.resolve(tenant,again.getId()).getSecretKey());
    }
    @Test void historyObjectMismatchRemainsUnboundAndReportsOnlyFailureCounts()throws Exception {
        var revision=service.ensure(tenant,"bootstrap",config("legacy",""),true);
        execute("INSERT INTO infra_file(id,tenant_id,provider,storage_uri,object_key,original_name,size) VALUES(887301,887101,'minio','minio://legacy/one','one','one',10)");
        when(client.headObject(any(),any())).thenReturn(StorageObjectMeta.builder().size(11).build());
        var plan=new StorageHistoryBindingJob.Plan();plan.setTenantId(tenant);plan.setRevisionId(revision.getId());plan.setSource("bootstrap");plan.setBucket("legacy");plan.setFrom(OffsetDateTime.now().minusDays(1));plan.setTo(OffsetDateTime.now().plusHours(1));
        var job=new StorageHistoryBindingJob(json,history,revisions,service,clients);var preview=job.process(plan);assertEquals(1,preview.rejected());
        plan.setExecute(true);plan.setConfirmPlan(preview.planHash());var report=job.process(plan);assertEquals(1,report.rejected());assertEquals(0,report.bound());assertEquals(1,revisions.unresolvedFiles(tenant));assertFalse(json.writeValueAsString(report).contains("secret-test"));
    }
    private void execute(String sql)throws Exception{try(Connection c=ds.getConnection();var st=c.createStatement()){st.executeUpdate(sql);}}
}
