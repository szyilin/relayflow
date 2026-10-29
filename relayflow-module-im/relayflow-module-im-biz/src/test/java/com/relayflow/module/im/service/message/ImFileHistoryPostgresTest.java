package com.relayflow.module.im.service.message;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.infra.api.file.*;
import com.relayflow.module.infra.api.file.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.Connection;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@EnabledIfEnvironmentVariable(named="RELAYFLOW_AUTH_TEST_JDBC_URL",matches="jdbc:postgresql:.*")
class ImFileHistoryPostgresTest {
 @Test void pagedHistoryResumesWithoutTextPageGapsAndOnlyBindsProvenSameTenantReferences() throws Exception {
  var ds=new DriverManagerDataSource(System.getenv("RELAYFLOW_AUTH_TEST_JDBC_URL"),System.getenv("RELAYFLOW_AUTH_TEST_JDBC_USER"),System.getenv("RELAYFLOW_AUTH_TEST_JDBC_PASSWORD"));
  try(Connection c=ds.getConnection();var st=c.createStatement()){
   st.executeUpdate("DELETE FROM im_message WHERE tenant_id=881009");
   st.executeUpdate("INSERT INTO im_message(id,tenant_id,conversation_id,sender_id,sender_type,type,content_json,seq) VALUES (883009,881009,884009,10,'user','text','{\"version\":1,\"blocks\":[{\"type\":\"text\",\"text\":\"legacy\"}]}',1),(883010,881009,884009,10,'user','file','{\"version\":1,\"blocks\":[{\"type\":\"file\",\"fileId\":\"12\"}]}',2),(883011,881009,884009,11,'user','file','{\"version\":1,\"blocks\":[{\"type\":\"file\",\"fileId\":\"12\"}]}',3)");
  }
  try {
   var cfg=new MybatisConfiguration();cfg.setMapUnderscoreToCamelCase(true);cfg.addMapper(ImMessageMapper.class);
   var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);factory.setConfiguration(cfg);
   var api=mock(FileApi.class);var f=new FileRespDTO();f.setId(12L);f.setTenantId(881009L);f.setCreator(10L);when(api.getFile(12L)).thenReturn(f);
   try(var session=factory.getObject().openSession(true)){
    var policy=new ImFileAccessPolicy(session.getMapper(ImMessageMapper.class),mock(ImConversationService.class),new ImContentHelper(new ObjectMapper(),api),api);
    var first=policy.scan(881009L,0,1);assertTrue(first.references().isEmpty());assertFalse(first.done());assertEquals(883009,first.nextId());
    var second=policy.scan(881009L,first.nextId(),1);assertEquals(1,second.references().size());assertTrue(policy.trusted(881009L,second.references().getFirst()));
    var third=policy.scan(881009L,second.nextId(),1);assertFalse(policy.trusted(881009L,third.references().getFirst()));
    f.setTenantId(881010L);assertFalse(policy.trusted(881009L,second.references().getFirst()));
    assertEquals(second,policy.scan(881009L,first.nextId(),1));
   }
  }finally{try(Connection c=ds.getConnection();var st=c.createStatement()){st.executeUpdate("DELETE FROM im_message WHERE tenant_id=881009");}}
 }
}
