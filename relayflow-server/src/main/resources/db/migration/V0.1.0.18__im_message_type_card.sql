-- Allow interactive card messages (im-bot-interactive-card).
ALTER TABLE im_message DROP CONSTRAINT ck_im_message_type;
ALTER TABLE im_message
    ADD CONSTRAINT ck_im_message_type CHECK (type IN ('text', 'image', 'file', 'system', 'card'));
