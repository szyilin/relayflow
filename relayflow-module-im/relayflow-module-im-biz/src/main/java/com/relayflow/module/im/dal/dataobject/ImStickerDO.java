package com.relayflow.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.BaseDO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("im_sticker")
public class ImStickerDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("pack_id")
    private Long packId;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("file_id")
    private Long fileId;

    @TableField("emoji")
    private String emoji;

    @TableField("sort")
    private Integer sort;
}
