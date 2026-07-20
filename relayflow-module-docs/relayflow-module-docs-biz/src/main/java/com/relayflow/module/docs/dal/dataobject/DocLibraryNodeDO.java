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
@TableName("doc_library_node")
public class DocLibraryNodeDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("object_id")
    private Long objectId;

    @TableField("sort_order")
    private Integer sortOrder;
}
