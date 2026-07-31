-- Ensure at most one ACTIVE rtc_session per owner (tenant + type + id + purpose).

-- Collapse duplicate ACTIVE rows: keep the newest by create_time, end the rest.
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY tenant_id, owner_type, owner_id, purpose
               ORDER BY create_time DESC NULLS LAST, id DESC
           ) AS rn
    FROM rtc_session
    WHERE deleted = 0
      AND status = 'ACTIVE'
)
UPDATE rtc_session s
SET status = 'ENDED',
    update_time = NOW()
FROM ranked r
WHERE s.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_rtc_session_active_owner
    ON rtc_session (tenant_id, owner_type, owner_id, purpose)
    WHERE deleted = 0 AND status = 'ACTIVE';
