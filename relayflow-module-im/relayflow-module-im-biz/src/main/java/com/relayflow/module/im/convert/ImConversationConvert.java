package com.relayflow.module.im.convert;

import com.relayflow.module.im.api.conversation.dto.ConversationSearchRespDTO;
import com.relayflow.module.im.controller.app.vo.ConversationItemRespVO;
import com.relayflow.module.im.service.conversation.model.ConversationListItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Collections;
import java.util.List;

@Mapper
public interface ImConversationConvert {

    ImConversationConvert INSTANCE = Mappers.getMapper(ImConversationConvert.class);

    ConversationItemRespVO toResp(ConversationListItem item);

    @Mapping(target = "conversationId", source = "id")
    @Mapping(target = "subtitle", source = "lastMsgPreview")
    ConversationSearchRespDTO toSearchDto(ConversationListItem item);

    default List<ConversationItemRespVO> toRespList(List<ConversationListItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().map(this::toResp).toList();
    }
}
