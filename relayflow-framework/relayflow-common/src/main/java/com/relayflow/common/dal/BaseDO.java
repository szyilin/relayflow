package com.relayflow.common.dal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Common audit fields for database entities.
 */
@Getter
@Setter
public abstract class BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long creator;

    private OffsetDateTime createTime;

    private Long updater;

    private OffsetDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
