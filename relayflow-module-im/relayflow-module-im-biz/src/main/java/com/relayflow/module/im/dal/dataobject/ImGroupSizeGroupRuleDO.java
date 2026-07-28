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
@TableName("im_group_size_group_rule")
public class ImGroupSizeGroupRuleDO extends TenantBaseDO {

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("member_limit")
    private Integer memberLimit;

    @TableField("sort_order")
    private Integer sortOrder;
}
