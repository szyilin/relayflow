package com.relayflow.module.im.api.card;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Handler outcome. {@code card} is a full replacement card block (map shape matching content block).
 * Null card means no message patch.
 */
@Data
@Builder
public class CardActionResult {

    private CardToast toast;
    /** Full card block fields (without requiring {@code type}); IM merges as {@code type=card}. */
    private Map<String, Object> card;

    @Data
    @Builder
    public static class CardToast {
        /** {@code success} | {@code info} | {@code warning} | {@code error} */
        private String type;
        private String content;
    }

    public static CardActionResult toastOnly(String type, String content) {
        return CardActionResult.builder()
                .toast(CardToast.builder().type(type).content(content).build())
                .build();
    }
}
