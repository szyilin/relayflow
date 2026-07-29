package com.relayflow.module.im.service.message;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.controller.app.vo.MessageReactionAggVO;
import com.relayflow.module.im.dal.dataobject.ImMessageReactionDO;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImMessageReactionServiceTest {

    @Test
    void normalizeEmojiTrimsAndRejectsBlank() {
        assertEquals("👍", ImMessageReactionService.normalizeEmoji(" 👍 "));
        ServiceException blank = assertThrows(ServiceException.class,
                () -> ImMessageReactionService.normalizeEmoji("  "));
        assertEquals(ErrorCodeConstants.MESSAGE_REACTION_INVALID.getCode(), blank.getCode());
    }

    @Test
    void normalizeEmojiRejectsTooLong() {
        String longEmoji = "a".repeat(33);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> ImMessageReactionService.normalizeEmoji(longEmoji));
        assertEquals(ErrorCodeConstants.MESSAGE_REACTION_INVALID.getCode(), ex.getCode());
    }

    @Test
    void toAggCountsAndMarksMe() {
        ImMessageReactionDO a = row(1L, "👍");
        ImMessageReactionDO b = row(2L, "👍");
        ImMessageReactionDO c = row(1L, "❤️");
        List<MessageReactionAggVO> agg = ImMessageReactionService.toAgg(List.of(a, b, c), 1L);
        assertEquals(2, agg.size());
        assertEquals("👍", agg.get(0).getEmoji());
        assertEquals(2, agg.get(0).getCount());
        assertTrue(agg.get(0).getMe());
        assertEquals("❤️", agg.get(1).getEmoji());
        assertEquals(1, agg.get(1).getCount());
        assertTrue(agg.get(1).getMe());

        List<MessageReactionAggVO> forOther = ImMessageReactionService.toAgg(List.of(a, b, c), 9L);
        assertFalse(forOther.get(0).getMe());
        assertFalse(forOther.get(1).getMe());
    }

    private static ImMessageReactionDO row(Long userId, String emoji) {
        ImMessageReactionDO row = new ImMessageReactionDO();
        row.setUserId(userId);
        row.setEmoji(emoji);
        return row;
    }
}
