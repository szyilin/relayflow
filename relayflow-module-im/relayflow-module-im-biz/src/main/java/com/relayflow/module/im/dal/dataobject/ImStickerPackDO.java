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
@TableName("im_sticker_pack")
public class ImStickerPackDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("scope")
    private String scope;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private Integer status;
}
