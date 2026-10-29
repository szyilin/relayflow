package com.relayflow.module.docs.service.document;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.relayflow.module.docs.dal.mapper.*;
import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="RELAYFLOW_AUTH_TEST_JDBC_URL",matches="jdbc:postgresql:.*")
class DocSavePostgresTest {
 private DriverManagerDataSource ds;private SqlSessionFactory factory;
 @BeforeEach void setup() throws Exception {
  ds=new DriverManagerDataSource(System.getenv("RELAYFLOW_AUTH_TEST_JDBC_URL"),System.getenv("RELAYFLOW_AUTH_TEST_JDBC_USER"),System.getenv("RELAYFLOW_AUTH_TEST_JDBC_PASSWORD"));
  var cfg=new MybatisConfiguration();cfg.setMapUnderscoreToCamelCase(true);cfg.addMapper(DocObjectMapper.class);cfg.addMapper(DocObjectExtMapper.class);
  var bean=new MybatisSqlSessionFactoryBean();bean.setDataSource(ds);bean.setConfiguration(cfg);bean.setTransactionFactory(new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory());factory=bean.getObject();
  execute("DELETE FROM doc_object WHERE id=883101");
  execute("INSERT INTO doc_object(id,tenant_id,type,title,body,body_format,content_version,owner_user_id,lifecycle) VALUES(883101,881101,'RICH_DOC','old','{\"type\":\"doc\"}','tiptap_json_v1',0,10,'ACTIVE')");
 }
 @AfterEach void cleanup()throws Exception{execute("DELETE FROM doc_object WHERE id=883101");}
 @Test void staleOpenAndRenameInterleaveWithCasInBothCommitOrders() throws Exception {
  for(boolean bodyFirst:new boolean[]{true,false}){
   execute("UPDATE doc_object SET title='old',content_version=0,body='{\"type\":\"doc\"}' WHERE id=883101");
   CountDownLatch loaded=new CountDownLatch(1),committed=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
   try{
    var metadata=pool.submit(()->{try(var s=factory.openSession(false)){
     s.getMapper(DocObjectMapper.class).selectById(883101L);loaded.countDown();if(bodyFirst)await(committed);
     var m=s.getMapper(DocObjectExtMapper.class);m.touchOpened(881101L,883101L,10L,OffsetDateTime.now());m.rename(881101L,883101L,"new title",10L,OffsetDateTime.now());s.commit();if(!bodyFirst)committed.countDown();
    }});
    var save=pool.submit(()->{await(loaded);if(!bodyFirst)await(committed);try(var s=factory.openSession(false)){
     assertEquals(1,s.getMapper(DocObjectExtMapper.class).saveBody(881101L,883101L,0,"{\"type\":\"doc\",\"content\":[]}",11L,OffsetDateTime.now()));s.commit();
    }if(bodyFirst)committed.countDown();});
    metadata.get(10,TimeUnit.SECONDS);save.get(10,TimeUnit.SECONDS);
   }finally{pool.shutdownNow();}
   try(var s=factory.openSession(true)){
    var row=s.getMapper(DocObjectMapper.class).selectById(883101L);assertEquals("new title",row.getTitle());assertEquals(1,row.getContentVersion());assertTrue(row.getBody().contains("content"));assertNotNull(row.getLastOpenedAt());
    assertEquals(0,s.getMapper(DocObjectExtMapper.class).saveBody(881101L,883101L,0,"{}",10L,OffsetDateTime.now()));
   }
  }
 }
 @Test void genericStaleEntityCannotWriteBodyVersionAndDeletedCannotSave() throws Exception {
  try(var s=factory.openSession(true)){
   var base=s.getMapper(DocObjectMapper.class);var stale=base.selectById(883101L);var ext=s.getMapper(DocObjectExtMapper.class);
   ext.saveBody(881101L,883101L,0,"{\"type\":\"doc\",\"content\":[]}",10L,OffsetDateTime.now());stale.setTitle("renamed");base.updateById(stale);
   assertEquals(1,base.selectById(883101L).getContentVersion());assertTrue(base.selectById(883101L).getBody().contains("content"));
   assertEquals(0,ext.saveBody(881102L,883101L,1,"{}",10L,OffsetDateTime.now()));
  }
  execute("UPDATE doc_object SET deleted=1,lifecycle='DELETED' WHERE id=883101");
  try(var s=factory.openSession(true)){assertEquals(0,s.getMapper(DocObjectExtMapper.class).saveBody(881101L,883101L,1,"{}",10L,OffsetDateTime.now()));}
 }
 private void execute(String sql)throws Exception{try(Connection c=ds.getConnection();var st=c.createStatement()){st.executeUpdate(sql);}}
 private static void await(CountDownLatch latch){try{if(!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("timeout");}catch(InterruptedException ex){throw new AssertionError(ex);}}
}
