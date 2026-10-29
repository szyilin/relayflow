package com.relayflow.module.infra.service.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.MemberIdentityProvider;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.infra.api.file.*;
import com.relayflow.module.infra.dal.dataobject.*;
import com.relayflow.module.infra.dal.mapper.InfraFileBindingMapper;
import com.relayflow.module.infra.job.PrivateFileBindingRepairJob;
import com.relayflow.module.infra.service.storage.StorageProviderService;
import com.relayflow.framework.oss.core.ObjectStorageClientFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class FileAuthorizationTest {
    private final FileService files = mock(FileService.class);
    private final InfraFileBindingMapper bindings = mock(InfraFileBindingMapper.class);
    private final DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
    private FileAuthorizationService auth;
    private InfraFileDO file;
    @BeforeEach void setup() {
        TenantContextHolder.set(1L);
        auth = new FileAuthorizationService(files, bindings, beans.getBeanProvider(FileAccessPolicy.class), (t,u)->0);
        file = new InfraFileDO(); file.setId(12L); file.setTenantId(1L); file.setCreator(10L); file.setAccessLevel("private");
        when(files.requireFile(12L)).thenReturn(file);
        when(bindings.selectList(any())).thenReturn(List.of());
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void unboundOnlyCreatorCanReadAndMissingCreatorDeniesAll() {
        assertSame(file, auth.requireReadable(1L,10L,12L));
        assertThrows(ServiceException.class,()->auth.requireReadable(1L,11L,12L));
        file.setCreator(null);
        assertThrows(ServiceException.class,()->auth.requireReadable(1L,10L,12L));
    }
    @Test void boundCreatorCannotBypassUnknownOrStaleDomain() {
        when(bindings.selectList(any())).thenReturn(List.of(ref("unknown",1L)));
        assertThrows(ServiceException.class,()->auth.requireReadable(1L,10L,12L));
        beans.registerSingleton("policy",policy(false,false));
        when(bindings.selectList(any())).thenReturn(List.of(ref("test",1L)));
        assertThrows(ServiceException.class,()->auth.requireReadable(1L,10L,12L));
    }
    @Test void independentValidReferenceWinsEvenIfAnotherFails() {
        beans.registerSingleton("policy",policy(true,true));
        when(bindings.selectList(any())).thenReturn(List.of(ref("test",1L),ref("test",2L)));
        assertSame(file,auth.requireReadable(1L,11L,12L));
    }
    @Test void crossTenantAndPolicyExceptionCannotProduceSignature() {
        var factory=mock(ObjectStorageClientFactory.class);
        var storage=mock(StorageProviderService.class);
        var download=new FileDownloadServiceImpl(files,auth,storage,factory);
        beans.registerSingleton("policy",policy(false,true));
        when(bindings.selectList(any())).thenReturn(List.of(ref("test",1L)));
        var login = new com.relayflow.framework.security.core.LoginUser(10L,"10",1L,"admin",List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null,login.getAuthorities()));
        try { assertThrows(ServiceException.class,()->download.resolveMemberDownload(12L)); }
        finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        verifyNoInteractions(factory,storage);
        assertThrows(ServiceException.class,()->auth.requireReadable(2L,10L,12L));
        file.setTenantId(2L);
        assertThrows(ServiceException.class,()->auth.requireReadable(1L,10L,12L));
    }
    @Test void historyDefaultsToDryRunAndExecuteIsExplicit() {
        var api=mock(FileApi.class);
        beans.registerSingleton("history",new FileHistorySource(){
            public String bizType(){return "test";}
            public Page scan(Long t,long a,int l){return new Page(3,true,List.of(new Reference(2L,12L,10L),new Reference(3L,13L,10L)));}
            public boolean trusted(Long t,Reference r){return r.fileId()==12L;}
        });
        var job=new PrivateFileBindingRepairJob(beans.getBeanProvider(FileHistorySource.class),api,new ObjectMapper());
        var plan=new PrivateFileBindingRepairJob.Plan(); plan.setTenantId(1L);plan.setBizType("test");
        var preview=job.repair(plan);
        assertEquals(1,preview.eligible());assertEquals(1,preview.rejected());verifyNoInteractions(api);
        plan.setExecute(true);job.repair(plan);job.repair(plan);
        verify(api,times(2)).bindFile(any());
        assertEquals(1L,TenantContextHolder.get());
    }
    private InfraFileBindingDO ref(String type,long id){var r=new InfraFileBindingDO();r.setBizType(type);r.setBizId(id);return r;}
    private FileAccessPolicy policy(boolean allow,boolean fails){return new FileAccessPolicy(){
        public String bizType(){return "test";}
        public boolean canRead(Long t,Long u,Long f,Long b){if(fails&&b==1L)throw new IllegalStateException();return allow;}
    };}
}
