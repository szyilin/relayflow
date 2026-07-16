package com.relayflow.module.im.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.relayflow.common.dal.BaseDO;
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
 * @since 2026-07-16
 */
@Getter
@Setter
@ToString
@TableName("im_bot")
public class ImBotDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("code")
    private String code;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("avatar_file_id")
    private Long avatarFileId;

    @TableField("scope")
    private String scope;

    /** {@code system} | {@code tenant} — see ImBotType. */
    @TableField("type")
    private String type;

    @TableField("enable_policy")
    private String enablePolicy;

    @TableField("handler_kind")
    private String handlerKind;

    @TableField("capabilities_json")
    private String capabilitiesJson;

    @TableField("status")
    private Integer status;
}
