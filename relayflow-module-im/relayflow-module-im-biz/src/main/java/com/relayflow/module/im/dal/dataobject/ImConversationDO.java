package com.relayflow.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.OffsetDateTime;
/**
 * <p>
 * 
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-16
 */
@Getter
@Setter
@ToString
@TableName("im_conversation")
public class ImConversationDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("type")
    private String type;

    @TableField("title")
    private String title;

    @TableField("avatar_file_id")
    private Long avatarFileId;

    @TableField("last_msg_id")
    private Long lastMsgId;

    @TableField("last_msg_at")
    private OffsetDateTime lastMsgAt;

    @TableField("last_msg_preview")
    private String lastMsgPreview;

    @TableField("settings_json")
    private String settingsJson;

    @TableField("direct_peer_low")
    private Long directPeerLow;

    @TableField("direct_peer_high")
    private Long directPeerHigh;

    @TableField("bot_peer_bot_id")
    private Long botPeerBotId;

    @TableField("bot_peer_user_id")
    private Long botPeerUserId;
}
