-- agent_session_message: persist UIMessage-like parts for stream replay
ALTER TABLE agent_session_message
    ADD COLUMN IF NOT EXISTS parts_json TEXT NULL;

COMMENT ON COLUMN agent_session_message.parts_json IS 'JSON array of message parts (text/reasoning/tool); null = legacy content-only';
