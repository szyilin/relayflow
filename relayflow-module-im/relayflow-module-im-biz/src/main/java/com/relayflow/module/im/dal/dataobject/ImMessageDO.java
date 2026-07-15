package com.relayflow.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
/**
 * <p>
 * 
 * </p>
 *
 * @author relayflow-codegen
 * @since 2026-07-15
 */
@Getter
@Setter
@ToString
@TableName("im_message")
public class ImMessageDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_id")
    private Long senderId;

    @TableField("sender_type")
    private String senderType;

    @TableField("type")
    private String type;

    @TableField("content_json")
    private String contentJson;

    @TableField("client_msg_id")
    private String clientMsgId;

    @TableField("seq")
    private Long seq;

    @TableField("reply_to_msg_id")
    private Long replyToMsgId;
}
