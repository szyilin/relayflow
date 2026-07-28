-- Official executor group for relayflow-server (PostgreSQL).
-- app_name MUST match relayflow.job.executor.appname. Idempotent.

INSERT INTO xxl_job_group (id, app_name, title, address_type, address_list, update_time)
SELECT 1, 'relayflow-server', 'RelayFlow', 0, NULL, NOW()
WHERE NOT EXISTS (SELECT 1 FROM xxl_job_group WHERE app_name = 'relayflow-server');

UPDATE xxl_job_group
SET title = 'RelayFlow',
    address_type = 0,
    update_time = NOW()
WHERE app_name = 'relayflow-server';

SELECT setval(pg_get_serial_sequence('xxl_job_group', 'id'), GREATEST((SELECT COALESCE(MAX(id), 1) FROM xxl_job_group), 1));
