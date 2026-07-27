-- Verification seed for workspace-contacts-enrich-api (NOT a Flyway migration).
-- Assumes default tenant_id=1 and admin user_id=1 (19988888888).
-- Peer user from local DB: 2077737353805094914 (adjust if your DB differs).

-- Pending add-contact request → admin
INSERT INTO sys_contact_request (
    id, tenant_id, from_user_id, to_user_id, status, subtitle,
    creator, create_time, updater, update_time, deleted
) VALUES (
    9100000000000000001, 1, 2077737353805094914, 1, 'pending', '想添加你为联系人',
    1, NOW(), 1, NOW(), 0
)
ON CONFLICT (id) DO NOTHING;

-- Optional: already-accepted request (should appear in request list, not re-acceptable)
INSERT INTO sys_contact_request (
    id, tenant_id, from_user_id, to_user_id, status, subtitle,
    creator, create_time, updater, update_time, deleted
) VALUES (
    9100000000000000002, 1, 2077737353805094914, 1, 'accepted', '通过群聊添加',
    1, NOW(), 1, NOW(), 0
)
ON CONFLICT (id) DO NOTHING;

-- Optional external row (accept of pending will also upsert)
INSERT INTO sys_contact_external (
    id, tenant_id, owner_user_id, target_user_id,
    creator, create_time, updater, update_time, deleted
) VALUES (
    9100000000000000011, 1, 1, 2077737353805094914,
    1, NOW(), 1, NOW(), 0
)
ON CONFLICT (id) DO NOTHING;

-- Optional star
INSERT INTO sys_contact_star (
    id, tenant_id, owner_user_id, target_user_id,
    creator, create_time, updater, update_time, deleted
) VALUES (
    9100000000000000021, 1, 1, 2077737353805094914,
    1, NOW(), 1, NOW(), 0
)
ON CONFLICT (id) DO NOTHING;
