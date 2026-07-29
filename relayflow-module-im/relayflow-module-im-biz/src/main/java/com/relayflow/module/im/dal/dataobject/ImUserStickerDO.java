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
@TableName("im_user_sticker")
public class ImUserStickerDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField("file_id")
    private Long fileId;

    @TableField("name")
    private String name;

    @TableField("sort")
    private Integer sort;

    @TableField("source_message_id")
    private Long sourceMessageId;
}
