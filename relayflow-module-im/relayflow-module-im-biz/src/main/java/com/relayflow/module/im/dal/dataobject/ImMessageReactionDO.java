package com.relayflow.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("im_message_reaction")
public class ImMessageReactionDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("message_id")
    private Long messageId;

    @TableField("user_id")
    private Long userId;

    @TableField("emoji")
    private String emoji;
}
