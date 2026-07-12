package com.relayflow.module.im.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class PresenceBatchRespVO {

    private List<PresenceItemRespVO> items;
}
