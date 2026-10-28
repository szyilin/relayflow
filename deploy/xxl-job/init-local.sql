-- Infrastructure metadata lives only in the scheduler DB, never in the business Flyway chain.
\o /dev/null
BEGIN;
SET LOCAL lock_timeout TO :'init_timeout';
SET LOCAL statement_timeout TO :'init_timeout';
SET LOCAL search_path TO public;
SELECT pg_advisory_xact_lock(hashtextextended('relayflow.local.xxl.bootstrap', 0));

SELECT to_regclass('public.relayflow_local_bootstrap') IS NOT NULL AS has_marker \gset
\set initialized false
\if :has_marker
    SELECT EXISTS (SELECT FROM relayflow_local_bootstrap WHERE component = 'xxl-job' AND version = 1) AS initialized,
           EXISTS (SELECT FROM relayflow_local_bootstrap WHERE component = 'xxl-job' AND version <> 1) AS unsupported
    \gset
    \if :unsupported
        DO $$ BEGIN RAISE EXCEPTION 'Unsupported initialization version; refusing to change scheduler data.'; END $$;
    \endif
\endif

\if :initialized
    SELECT count(*) = 9 AS schema_intact FROM pg_tables WHERE schemaname = 'public'
        AND tablename IN ('xxl_job_group', 'xxl_job_registry', 'xxl_job_info', 'xxl_job_logglue',
            'xxl_job_log', 'xxl_job_log_report', 'xxl_job_lock', 'xxl_job_user', 'relayflow_local_bootstrap')
    \gset
    \if :schema_intact
        \echo [xxl-job-init] Already initialized; preserving jobs, schedules, users and executor settings.
    \else
        DO $$ BEGIN RAISE EXCEPTION 'Initialized schema is incomplete; restore it rather than reseeding over existing data.'; END $$;
    \endif
\else
    SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'xxl_job_%') AS existing_tables
    \gset
    \if :existing_tables
        DO $$ BEGIN RAISE EXCEPTION 'Existing unmarked scheduler tables found; refusing automatic adoption or seed overwrite.'; END $$;
    \endif

    CREATE TABLE IF NOT EXISTS relayflow_local_bootstrap (
        component TEXT PRIMARY KEY,
        version INTEGER NOT NULL,
        completed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );
    \ir schema/01_tables_xxl_job.sql
    \ir seed/01_executor.sql
    \ir seed/02_jobs.sql
    INSERT INTO relayflow_local_bootstrap (component, version) VALUES ('xxl-job', 1);
    \echo [xxl-job-init] First installation prepared; committing schema, official seeds and marker together.
\endif
COMMIT;
\echo [xxl-job-init] Scheduler ready.
