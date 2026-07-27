package com.relayflow.module.docs.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.TenantBaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@TableName("doc_user_visit")
public class DocUserVisitDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField("object_id")
    private Long objectId;

    @TableField("last_opened_at")
    private OffsetDateTime lastOpenedAt;

    @TableField("title_snapshot")
    private String titleSnapshot;

    /** OWNER | SHARED */
    @TableField("source")
    private String source;
}
