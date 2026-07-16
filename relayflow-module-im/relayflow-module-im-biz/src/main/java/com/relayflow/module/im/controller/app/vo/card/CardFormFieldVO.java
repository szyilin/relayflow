package com.relayflow.module.im.controller.app.vo.card;

import lombok.Data;

@Data
public class CardFormFieldVO {

    private String name;
    private String label;
    private Boolean required;
    private String control;
}
