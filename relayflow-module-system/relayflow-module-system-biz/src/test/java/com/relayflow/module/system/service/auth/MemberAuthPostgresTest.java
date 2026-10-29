package com.relayflow.module.system.service.auth;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.relayflow.framework.security.core.IdentityValidationException;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantUserExtMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.enums.TenantUserStatus;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Uses a migrated, isolated PostgreSQL database; never connects to the normal application datasource. */
@EnabledIfEnvironmentVariable(named = "RELAYFLOW_AUTH_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
class MemberAuthPostgresTest {
    private static final long TENANT = 881001;
    private static final long USER = 882001;
    private DriverManagerDataSource dataSource;
    private SqlSessionFactory sessions;

    @BeforeEach
    void setup() throws Exception {
        dataSource = new DriverManagerDataSource(System.getenv("RELAYFLOW_AUTH_TEST_JDBC_URL"),
                System.getenv("RELAYFLOW_AUTH_TEST_JDBC_USER"), System.getenv("RELAYFLOW_AUTH_TEST_JDBC_PASSWORD"));
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(SysTenantUserMapper.class);
        configuration.addMapper(SysTenantUserExtMapper.class);
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionFactory(new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory());
        factory.setConfiguration(configuration);
        sessions = factory.getObject();
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM sys_tenant_user WHERE tenant_id IN (881001, 881002)");
            statement.executeUpdate("INSERT INTO sys_tenant_user (id, tenant_id, user_id, status, nickname, signature) "
                    + "VALUES (883001, 881001, 882001, 'ACTIVE', 'old', 'keep'), "
                    + "(883002, 881002, 882001, 'ACTIVE', 'other', 'keep')");
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM sys_tenant_user WHERE tenant_id IN (881001, 881002)");
        }
    }

    @Test
    void disableRetryRollbackAndReenableAreAtomicAndTenantScoped() {
        try (SqlSession session = sessions.openSession(false)) {
            var mapper = session.getMapper(SysTenantUserExtMapper.class);
            assertEquals(1, mapper.updateStatus(TENANT, USER, TenantUserStatus.SUSPENDED, null));
            session.rollback();
        }
        assertState(TenantUserStatus.ACTIVE, 0);
        try (SqlSession session = sessions.openSession(true)) {
            var mapper = session.getMapper(SysTenantUserExtMapper.class);
            assertEquals(1, mapper.updateStatus(TENANT, USER, TenantUserStatus.SUSPENDED, null));
            assertEquals(0, mapper.updateStatus(TENANT, USER, TenantUserStatus.SUSPENDED, null));
            assertThrows(IdentityValidationException.class, () -> new DatabaseMemberIdentityProvider(
                    session.getMapper(SysTenantUserExtMapper.class)).requireActiveVersion(TENANT, USER));
            assertEquals(0, mapper.updateStatus(TENANT, USER, TenantUserStatus.ACTIVE, TenantUserStatus.NOT_JOINED));
        }
        assertState(TenantUserStatus.SUSPENDED, 1);
        try (SqlSession session = sessions.openSession(true)) {
            var mapper = session.getMapper(SysTenantUserExtMapper.class);
            mapper.updateStatus(TENANT, USER, TenantUserStatus.ACTIVE, null);
            var identity = new DatabaseMemberIdentityProvider(session.getMapper(SysTenantUserExtMapper.class));
            assertEquals(1, identity.requireActiveVersion(TENANT, USER));
            assertEquals(0, identity.requireActiveVersion(881002L, USER));
        }
    }

    @Test
    void repeatedIdentityChecksInOneTransactionObserveExternallyCommittedInvalidation() {
        try (SqlSession reader = sessions.openSession(false)) {
            var identity = new DatabaseMemberIdentityProvider(reader.getMapper(SysTenantUserExtMapper.class));
            assertEquals(0, identity.requireActiveVersion(TENANT, USER));
            try (SqlSession writer = sessions.openSession(true)) {
                writer.getMapper(SysTenantUserExtMapper.class).updateStatus(TENANT, USER, TenantUserStatus.SUSPENDED, null);
            }
            assertEquals(403, assertThrows(IdentityValidationException.class,
                    () -> identity.requireActiveVersion(TENANT, USER)).getHttpStatus());
            try (SqlSession writer = sessions.openSession(true)) {
                writer.getMapper(SysTenantUserExtMapper.class).updateStatus(TENANT, USER, TenantUserStatus.ACTIVE, null);
            }
            assertEquals(1, identity.requireActiveVersion(TENANT, USER));
            reader.rollback();
        }
    }

    @Test
    void staleGenericUpdateCannotRestoreStatusOrVersion() {
        SysTenantUserDO stale;
        try (SqlSession session = sessions.openSession(true)) {
            stale = session.getMapper(SysTenantUserMapper.class).selectById(883001L);
        }
        try (SqlSession session = sessions.openSession(true)) {
            session.getMapper(SysTenantUserExtMapper.class).updateStatus(TENANT, USER, TenantUserStatus.SUSPENDED, null);
        }
        stale.setNickname("changed");
        stale.setAuthVersion(0L);
        try (SqlSession session = sessions.openSession(true)) {
            session.getMapper(SysTenantUserMapper.class).updateById(stale);
        }
        assertState(TenantUserStatus.SUSPENDED, 1);
    }

    @Test
    void concurrentProfileAndDisableCommitInEitherOrderWithoutLostInvalidation() throws Exception {
        for (boolean disableFirst : new boolean[]{true, false}) {
            try (SqlSession session = sessions.openSession(true)) {
                session.getMapper(SysTenantUserExtMapper.class).updateStatus(TENANT, USER, TenantUserStatus.ACTIVE, null);
            }
            long before;
            try (SqlSession session = sessions.openSession(true)) {
                before = session.getMapper(SysTenantUserMapper.class).selectById(883001L).getAuthVersion();
            }
            CountDownLatch staleRead = new CountDownLatch(1);
            CountDownLatch firstCommitted = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> profile = executor.submit(() -> {
                    try (SqlSession session = sessions.openSession(false)) {
                        session.getMapper(SysTenantUserMapper.class).selectById(883001L);
                        staleRead.countDown();
                        if (disableFirst) await(firstCommitted);
                        Map<String, Object> changes = new HashMap<>();
                        changes.put("nickname", "parallel");
                        changes.put("avatar", null); // Explicit clear remains supported.
                        session.getMapper(SysTenantUserExtMapper.class).updateProfile(TENANT, USER, changes);
                        session.commit();
                        if (!disableFirst) firstCommitted.countDown();
                    }
                });
                Future<?> disable = executor.submit(() -> {
                    await(staleRead);
                    if (!disableFirst) await(firstCommitted);
                    try (SqlSession session = sessions.openSession(false)) {
                        session.getMapper(SysTenantUserExtMapper.class).updateStatus(TENANT, USER, TenantUserStatus.SUSPENDED, null);
                        session.commit();
                    }
                    if (disableFirst) firstCommitted.countDown();
                });
                profile.get(10, TimeUnit.SECONDS);
                disable.get(10, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }
            assertState(TenantUserStatus.SUSPENDED, before + 1);
            try (SqlSession session = sessions.openSession(true)) {
                SysTenantUserDO member = session.getMapper(SysTenantUserMapper.class).selectById(883001L);
                assertEquals("parallel", member.getNickname());
                assertEquals("keep", member.getSignature());
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Concurrent test timed out");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private void assertState(TenantUserStatus status, long version) {
        try (SqlSession session = sessions.openSession(true)) {
            SysTenantUserDO member = session.getMapper(SysTenantUserMapper.class).selectById(883001L);
            assertEquals(status, member.getStatus());
            assertEquals(version, member.getAuthVersion());
        }
    }
}
