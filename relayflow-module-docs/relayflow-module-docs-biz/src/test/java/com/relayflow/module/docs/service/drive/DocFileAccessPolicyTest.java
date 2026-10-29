package com.relayflow.module.docs.service.drive;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.docs.dal.mapper.DocObjectMapper;
import com.relayflow.module.infra.api.file.FileApi;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DocFileAccessPolicyTest {
 @Test void ownershipLifecycleAndFileIdentityAreCurrent(){
  var mapper=mock(DocObjectMapper.class);var row=new DocObjectDO();row.setTenantId(1L);row.setOwnerUserId(10L);
  row.setType("FILE");row.setLifecycle("ACTIVE");row.setStorageFileId(12L);
  when(mapper.selectById(1L)).thenReturn(row);
  var policy=new DocFileAccessPolicy(mapper,mock(FileApi.class));
  assertTrue(policy.canRead(1L,10L,12L,1L));row.setOwnerUserId(11L);
  assertFalse(policy.canRead(1L,10L,12L,1L));assertTrue(policy.canRead(1L,11L,12L,1L));
  assertFalse(policy.canRead(2L,11L,12L,1L));assertFalse(policy.canRead(1L,11L,13L,1L));
  row.setLifecycle("DELETED");assertFalse(policy.canRead(1L,11L,12L,1L));
  row.setLifecycle("ACTIVE");row.setType("RICH_DOC");assertFalse(policy.canRead(1L,11L,12L,1L));
 }
}
