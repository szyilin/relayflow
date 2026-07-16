package com.relayflow.module.im.controller.app.vo.card;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CardBehaviorVO {

    private String type;
    private String route;
    private String actionKey;
    private Map<String, Object> payload;
    private List<CardFormFieldVO> form;
}
