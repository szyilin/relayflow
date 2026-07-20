package com.relayflow.module.docs.dal.dataobject;

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
 * @since 2026-07-20
 */
@Getter
@Setter
@ToString
@TableName("doc_drive_item")
public class DocDriveItemDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("folder_id")
    private Long folderId;

    @TableField("object_id")
    private Long objectId;

    @TableField("sort_order")
    private Integer sortOrder;
}
