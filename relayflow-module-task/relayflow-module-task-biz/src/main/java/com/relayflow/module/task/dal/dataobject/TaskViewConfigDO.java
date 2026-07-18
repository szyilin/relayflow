package com.relayflow.module.task.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName(value = "task_view_config", autoResultMap = true)
public class TaskViewConfigDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("context_type")
    private String contextType;

    @TableField("context_id")
    private Long contextId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField(value = "config_json", typeHandler = JsonbTypeHandler.class)
    private String configJson;
}
