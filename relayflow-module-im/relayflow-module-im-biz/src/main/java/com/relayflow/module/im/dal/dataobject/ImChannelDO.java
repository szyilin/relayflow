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
@TableName("im_channel")
public class ImChannelDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("post_permission")
    private String postPermission;

    @TableField("owner_user_id")
    private Long ownerUserId;
}
