-- Official scheduled jobs (warehouse source of truth, PostgreSQL).
-- Portable contract: executor_handler + Cron + description.
-- Add new jobs here + matching @XxlJob handler in *-biz.

INSERT INTO xxl_job_info (
  job_group, job_desc, add_time, update_time, author, alarm_email,
  schedule_type, schedule_conf, misfire_strategy, executor_route_strategy,
  executor_handler, executor_param, executor_block_strategy, executor_timeout,
  executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime,
  child_jobid, trigger_status, trigger_last_time, trigger_next_time
)
SELECT
  g.id,
  '中国行政区划月同步',
  NOW(), NOW(), 'relayflow', '',
  'CRON', '0 0 3 1 * ?', 'DO_NOTHING', 'FIRST',
  'infraGeoSyncChina', '', 'SERIAL_EXECUTION', 0,
  1, 'BEAN', '', 'GLUE代码初始化', NOW(),
  '', 1, 0, 0
FROM xxl_job_group g
WHERE g.app_name = 'relayflow-server'
  AND NOT EXISTS (
    SELECT 1 FROM xxl_job_info i WHERE i.executor_handler = 'infraGeoSyncChina'
  )
LIMIT 1;

UPDATE xxl_job_info i
SET
  job_desc = '中国行政区划月同步',
  schedule_type = 'CRON',
  schedule_conf = '0 0 3 1 * ?',
  misfire_strategy = 'DO_NOTHING',
  executor_route_strategy = 'FIRST',
  executor_block_strategy = 'SERIAL_EXECUTION',
  glue_type = 'BEAN',
  author = 'relayflow',
  trigger_status = 1,
  update_time = NOW(),
  job_group = g.id
FROM xxl_job_group g
WHERE g.app_name = 'relayflow-server'
  AND i.executor_handler = 'infraGeoSyncChina';

SELECT setval(pg_get_serial_sequence('xxl_job_info', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM xxl_job_info), 1));

INSERT INTO xxl_job_info (
  job_group, job_desc, add_time, update_time, author, alarm_email,
  schedule_type, schedule_conf, misfire_strategy, executor_route_strategy,
  executor_handler, executor_param, executor_block_strategy, executor_timeout,
  executor_fail_retry_count, glue_type, glue_source, glue_remark, glue_updatetime,
  child_jobid, trigger_status, trigger_last_time, trigger_next_time
)
SELECT
  g.id,
  '智能体自动化到期扫描',
  NOW(), NOW(), 'relayflow', '',
  'CRON', '0 * * * * ?', 'DO_NOTHING', 'FIRST',
  'agentAutomationTick', '', 'SERIAL_EXECUTION', 0,
  0, 'BEAN', '', 'GLUE代码初始化', NOW(),
  '', 1, 0, 0
FROM xxl_job_group g
WHERE g.app_name = 'relayflow-server'
  AND NOT EXISTS (
    SELECT 1 FROM xxl_job_info i WHERE i.executor_handler = 'agentAutomationTick'
  )
LIMIT 1;

UPDATE xxl_job_info i
SET
  job_desc = '智能体自动化到期扫描',
  schedule_type = 'CRON',
  schedule_conf = '0 * * * * ?',
  misfire_strategy = 'DO_NOTHING',
  executor_route_strategy = 'FIRST',
  executor_block_strategy = 'SERIAL_EXECUTION',
  glue_type = 'BEAN',
  author = 'relayflow',
  trigger_status = 1,
  update_time = NOW(),
  job_group = g.id
FROM xxl_job_group g
WHERE g.app_name = 'relayflow-server'
  AND i.executor_handler = 'agentAutomationTick';

SELECT setval(pg_get_serial_sequence('xxl_job_info', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM xxl_job_info), 1));
