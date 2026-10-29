package com.relayflow.module.im.service.message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.infra.api.file.FileApi;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ImFileAccessPolicyTest {
 @Test void currentContentRecallAndMembershipGateTheBinding(){
  var mapper=mock(ImMessageMapper.class);var conv=mock(ImConversationService.class);var api=mock(FileApi.class);
  var helper=new ImContentHelper(new ObjectMapper(),api);var row=new ImMessageDO();row.setTenantId(1L);row.setConversationId(2L);
  row.setContentJson("{\"version\":1,\"blocks\":[{\"type\":\"file\",\"fileId\":\"12\"}]}");
  when(mapper.selectById(1L)).thenReturn(row);var policy=new ImFileAccessPolicy(mapper,conv,helper,api);
  assertTrue(policy.canRead(1L,10L,12L,1L));assertFalse(policy.canRead(2L,10L,12L,1L));
  row.setRecalledAt(OffsetDateTime.now());assertFalse(policy.canRead(1L,10L,12L,1L));
  row.setRecalledAt(null);row.setContentJson("{\"version\":1,\"blocks\":[]}");assertFalse(policy.canRead(1L,10L,12L,1L));
 }
 @Test void everyReferenceMustAlreadyBeReadableBeforeBindingsAreCreated(){
  var api=mock(FileApi.class);var helper=new ImContentHelper(new ObjectMapper(),api);
  var content=helper.fromJson("{\"version\":1,\"blocks\":[{\"type\":\"file\",\"fileId\":\"12\"},{\"type\":\"sticker\",\"fileId\":\"13\"}]}");
  doThrow(new IllegalStateException()).when(api).requireReadable(1L,10L,13L);
  assertThrows(IllegalStateException.class,()->helper.requireReferences(1L,10L,content));
  verify(api,never()).bindFile(any());
 }
}
