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
@TableName("im_conversation_member")
public class ImConversationMemberDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("subject_id")
    private Long subjectId;

    @TableField("role")
    private String role;

    @TableField("read_seq")
    private Long readSeq;

    @TableField("unread_count")
    private Integer unreadCount;

    @TableField("join_time")
    private OffsetDateTime joinTime;

    @TableField("mute_until")
    private OffsetDateTime muteUntil;

    @TableField("pinned")
    private Integer pinned;

    @TableField("subject_type")
    private String subjectType;
}
