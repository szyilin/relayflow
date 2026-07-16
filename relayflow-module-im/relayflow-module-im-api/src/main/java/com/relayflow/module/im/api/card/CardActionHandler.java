package com.relayflow.module.im.api.card;

/**
 * In-process card action SPI. Business modules implement and register as Spring beans.
 * IM resolves by {@link #actionKey()} and does not interpret payload fields.
 */
public interface CardActionHandler {

    /** Stable key, e.g. {@code system.member.invite.accept}, {@code bpm.approval.approve}. */
    String actionKey();

    CardActionResult handle(CardActionContext context);
}
