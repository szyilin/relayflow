package com.relayflow.module.im.api.bot.dto;

import lombok.Data;

@Data
public class ImBotSendTarget {

    public static final String SCOPE_SINGLE = "SINGLE";
    public static final String SCOPE_ALL_ACTIVE_MEMBERSHIPS = "ALL_ACTIVE_MEMBERSHIPS";

    /**
     * {@link #SCOPE_SINGLE} (default) or {@link #SCOPE_ALL_ACTIVE_MEMBERSHIPS}.
     */
    private String scope;

    /** Required when scope is {@link #SCOPE_SINGLE}. */
    private Long tenantId;

    /** Target user id (always required). */
    private Long userId;
}
