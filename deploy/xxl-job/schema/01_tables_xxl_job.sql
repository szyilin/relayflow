-- XXL-JOB 3.3.x tables for PostgreSQL (separate database: xxl_job).
-- Not mixed into the RelayFlow business DB. Idempotent for re-runs.
-- Admin default: admin / 123456 (SHA256 hex, XXL-JOB 3.x).

CREATE TABLE IF NOT EXISTS xxl_job_group (
    id           SERIAL PRIMARY KEY,
    app_name     VARCHAR(64)  NOT NULL,
    title        VARCHAR(12)  NOT NULL,
    address_type SMALLINT     NOT NULL DEFAULT 0,
    address_list TEXT,
    update_time  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS xxl_job_registry (
    id             SERIAL PRIMARY KEY,
    registry_group VARCHAR(50)  NOT NULL,
    registry_key   VARCHAR(255) NOT NULL,
    registry_value VARCHAR(255) NOT NULL,
    update_time    TIMESTAMP,
    CONSTRAINT uk_xxl_job_registry_gkv UNIQUE (registry_group, registry_key, registry_value)
);

CREATE TABLE IF NOT EXISTS xxl_job_info (
    id                        SERIAL PRIMARY KEY,
    job_group                 INT          NOT NULL,
    job_desc                  VARCHAR(255) NOT NULL,
    add_time                  TIMESTAMP,
    update_time               TIMESTAMP,
    author                    VARCHAR(64),
    alarm_email               VARCHAR(255),
    schedule_type             VARCHAR(50)  NOT NULL DEFAULT 'NONE',
    schedule_conf             VARCHAR(128),
    misfire_strategy          VARCHAR(50)  NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy   VARCHAR(50),
    executor_handler          VARCHAR(255),
    executor_param            VARCHAR(512),
    executor_block_strategy   VARCHAR(50),
    executor_timeout          INT          NOT NULL DEFAULT 0,
    executor_fail_retry_count INT          NOT NULL DEFAULT 0,
    glue_type                 VARCHAR(50)  NOT NULL,
    glue_source               TEXT,
    glue_remark               VARCHAR(128),
    glue_updatetime           TIMESTAMP,
    child_jobid               VARCHAR(255),
    trigger_status            SMALLINT     NOT NULL DEFAULT 0,
    trigger_last_time         BIGINT       NOT NULL DEFAULT 0,
    trigger_next_time         BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS xxl_job_logglue (
    id          SERIAL PRIMARY KEY,
    job_id      INT          NOT NULL,
    glue_type   VARCHAR(50),
    glue_source TEXT,
    glue_remark VARCHAR(128) NOT NULL,
    add_time    TIMESTAMP,
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS xxl_job_log (
    id                        BIGSERIAL PRIMARY KEY,
    job_group                 INT       NOT NULL,
    job_id                    INT       NOT NULL,
    executor_address          VARCHAR(255),
    executor_handler          VARCHAR(255),
    executor_param            VARCHAR(512),
    executor_sharding_param   VARCHAR(20),
    executor_fail_retry_count INT       NOT NULL DEFAULT 0,
    trigger_time              TIMESTAMP,
    trigger_code              INT       NOT NULL,
    trigger_msg               TEXT,
    handle_time               TIMESTAMP,
    handle_code               INT       NOT NULL,
    handle_msg                TEXT,
    alarm_status              SMALLINT  NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS i_xxl_job_log_trigger_time ON xxl_job_log (trigger_time);
CREATE INDEX IF NOT EXISTS i_xxl_job_log_handle_code ON xxl_job_log (handle_code);
CREATE INDEX IF NOT EXISTS i_xxl_job_log_jobid_group ON xxl_job_log (job_id, job_group);
CREATE INDEX IF NOT EXISTS i_xxl_job_log_job_id ON xxl_job_log (job_id);

CREATE TABLE IF NOT EXISTS xxl_job_log_report (
    id            SERIAL PRIMARY KEY,
    trigger_day   TIMESTAMP,
    running_count INT NOT NULL DEFAULT 0,
    suc_count     INT NOT NULL DEFAULT 0,
    fail_count    INT NOT NULL DEFAULT 0,
    update_time   TIMESTAMP,
    CONSTRAINT uk_xxl_job_log_report_day UNIQUE (trigger_day)
);

CREATE TABLE IF NOT EXISTS xxl_job_lock (
    lock_name VARCHAR(50) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS xxl_job_user (
    id         SERIAL PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,
    token      VARCHAR(100),
    role       SMALLINT     NOT NULL,
    permission VARCHAR(255),
    CONSTRAINT uk_xxl_job_user_username UNIQUE (username)
);

INSERT INTO xxl_job_user (id, username, password, role, permission)
SELECT 1, 'admin', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92', 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_user WHERE username = 'admin');

INSERT INTO xxl_job_lock (lock_name)
SELECT 'schedule_lock'
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_lock WHERE lock_name = 'schedule_lock');

SELECT setval(pg_get_serial_sequence('xxl_job_user', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM xxl_job_user), 1));
