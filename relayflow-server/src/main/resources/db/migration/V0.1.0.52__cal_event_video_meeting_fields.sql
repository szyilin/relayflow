-- Calendar video-meeting link fields (Feishu-style: event is source of truth).

ALTER TABLE cal_event
    ADD COLUMN video_meeting_kind VARCHAR(16) NOT NULL DEFAULT 'NONE',
    ADD COLUMN external_meeting_url VARCHAR(1000);

ALTER TABLE cal_event
    ADD CONSTRAINT ck_cal_event_video_meeting_kind
        CHECK (video_meeting_kind IN ('NONE', 'RELAYFLOW', 'EXTERNAL'));

COMMENT ON COLUMN cal_event.video_meeting_kind IS 'NONE | RELAYFLOW | EXTERNAL';
COMMENT ON COLUMN cal_event.external_meeting_url IS 'External meeting URL when kind=EXTERNAL';
