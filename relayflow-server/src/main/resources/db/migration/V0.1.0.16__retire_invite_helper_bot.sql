-- Retire invite-helper; org-assistant owns organization invites.

UPDATE im_bot
SET description = '组织与角色、成员邀请等相关提醒',
    update_time = NOW()
WHERE code = 'org-assistant'
  AND deleted = 0;

UPDATE im_bot_user_enablement
SET deleted = 1,
    update_time = NOW()
WHERE bot_id = (SELECT id FROM im_bot WHERE code = 'invite-helper' LIMIT 1)
  AND deleted = 0;

UPDATE im_bot_tenant_enablement
SET deleted = 1,
    update_time = NOW()
WHERE bot_id = (SELECT id FROM im_bot WHERE code = 'invite-helper' LIMIT 1)
  AND deleted = 0;

UPDATE im_bot
SET deleted = 1,
    status = 0,
    update_time = NOW()
WHERE code = 'invite-helper'
  AND deleted = 0;
