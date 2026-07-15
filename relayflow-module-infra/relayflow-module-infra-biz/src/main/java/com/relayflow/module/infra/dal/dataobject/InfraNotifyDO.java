package com.relayflow.module.infra.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler;
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
@TableName("infra_notify")
public class InfraNotifyDO extends TenantBaseDO {

    private static final long serialVersionUID = 1L;

    @TableField("user_id")
    private Long userId;

    @TableField("mobile")
    private String mobile;

    @TableField("type")
    private String type;

    @TableField("title")
    private String title;

    @TableField("body")
    private String body;

    @TableField(value = "payload_json", typeHandler = JsonbTypeHandler.class)
    private String payloadJson;

    @TableField("read_flag")
    private Integer readFlag;

    @TableField("dedupe_key")
    private String dedupeKey;
}
