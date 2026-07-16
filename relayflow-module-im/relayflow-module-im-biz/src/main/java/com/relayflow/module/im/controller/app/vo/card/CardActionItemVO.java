package com.relayflow.module.im.controller.app.vo.card;

import lombok.Data;

@Data
public class CardActionItemVO {

    private String id;
    private String label;
    private String style;
    private CardBehaviorVO behavior;
}
