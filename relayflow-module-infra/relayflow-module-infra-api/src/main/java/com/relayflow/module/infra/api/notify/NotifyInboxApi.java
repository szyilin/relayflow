package com.relayflow.module.infra.api.notify;

import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;

public interface NotifyInboxApi {

    /**
     * Push or idempotently refresh an unread notification of the same tenant + receiver + type.
     */
    Long push(NotifyItemCommand command);

    /**
     * Associate orphan mobile notifications with a registered user id.
     */
    void backfillUserIdByMobile(String mobile, Long userId);
}
