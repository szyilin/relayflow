-- Task collab P1: followers, comments, activity feed.

CREATE TABLE task_follower (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_follower PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_task_follower_task_user
    ON task_follower (tenant_id, task_id, user_id)
    WHERE deleted = 0;

CREATE INDEX idx_task_follower_user
    ON task_follower (tenant_id, user_id)
    WHERE deleted = 0;

COMMENT ON TABLE task_follower IS 'Task followers (我关注的)';
COMMENT ON COLUMN task_follower.task_id IS 'task_item.id';
COMMENT ON COLUMN task_follower.user_id IS 'Follower user id';

CREATE TABLE task_comment (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    author_id       BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_comment PRIMARY KEY (id)
);

CREATE INDEX idx_task_comment_task
    ON task_comment (tenant_id, task_id, create_time)
    WHERE deleted = 0;

COMMENT ON TABLE task_comment IS 'Task plain-text comments';
COMMENT ON COLUMN task_comment.author_id IS 'Comment author user id';

CREATE TABLE task_activity (
    id              BIGINT          NOT NULL,
    tenant_id       BIGINT          NOT NULL,
    task_id         BIGINT          NOT NULL,
    task_title      VARCHAR(200)    NOT NULL,
    actor_id        BIGINT          NOT NULL,
    type            VARCHAR(32)     NOT NULL,
    summary         VARCHAR(500)    NOT NULL,
    creator         BIGINT,
    create_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updater         BIGINT,
    update_time     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT pk_task_activity PRIMARY KEY (id)
);

CREATE INDEX idx_task_activity_task
    ON task_activity (tenant_id, task_id, create_time DESC)
    WHERE deleted = 0;

CREATE INDEX idx_task_activity_create
    ON task_activity (tenant_id, create_time DESC)
    WHERE deleted = 0;

COMMENT ON TABLE task_activity IS 'Task activity / dynamics feed';
COMMENT ON COLUMN task_activity.type IS 'created|field_changed|subtask_*|follower_*|commented|assigned';
COMMENT ON COLUMN task_activity.summary IS 'Human-readable activity summary';
