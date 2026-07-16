package com.relayflow.module.im.service.bot.ingress;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.service.bot.runtime.BotInboundContext;
import com.relayflow.module.im.service.bot.runtime.BotRuntime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotIngressImpl implements BotIngress {

    private static final int BOT_STATUS_ENABLED = 1;

    private final ImBotMapper botMapper;
    private final BotRuntime botRuntime;

    @Override
    public void onInbound(BotInboundContext context) {
        if (context == null) {
            return;
        }
        try {
            BotInboundContext enriched = enrich(context);
            if (enriched == null) {
                return;
            }
            botRuntime.dispatch(enriched);
        } catch (Exception ex) {
            log.warn("BotIngress.onInbound failed: botCode={}, conversationId={}",
                    context.getBotCode(), context.getConversationId(), ex);
        }
    }

    private BotInboundContext enrich(BotInboundContext context) {
        ImBotDO bot = null;
        if (context.getBotId() != null) {
            bot = botMapper.selectById(context.getBotId());
        } else if (StringUtils.hasText(context.getBotCode())) {
            bot = botMapper.selectOne(Wrappers.<ImBotDO>lambdaQuery()
                    .eq(ImBotDO::getCode, context.getBotCode().trim())
                    .eq(ImBotDO::getStatus, BOT_STATUS_ENABLED));
        }
        if (bot == null || !Objects.equals(bot.getStatus(), BOT_STATUS_ENABLED)) {
            log.warn("BotIngress: bot not found or disabled code={} id={}",
                    context.getBotCode(), context.getBotId());
            return null;
        }
        return BotInboundContext.builder()
                .tenantId(context.getTenantId())
                .conversationId(context.getConversationId())
                .botId(bot.getId())
                .botCode(bot.getCode())
                .handlerKind(bot.getHandlerKind())
                .botName(bot.getName())
                .triggerUserId(context.getTriggerUserId())
                .triggerMessageId(context.getTriggerMessageId())
                .inboundText(context.getInboundText())
                .build();
    }
}
