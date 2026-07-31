package com.relayflow.module.im.service.tag;

import com.relayflow.module.im.controller.app.vo.ImUserTagItemRespVO;
import com.relayflow.module.im.dal.dataobject.ImUserTagDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ImUserTagService {

    List<ImUserTagItemRespVO> listMyTags();

    ImUserTagItemRespVO createTag(String name);

    void assignTag(Long conversationId, Long tagId);

    ImUserTagItemRespVO createAndAssign(Long conversationId, String name);

    /** tagId list per conversation for the given user (only assigned tags). */
    Map<Long, List<Long>> listTagIdsByConversation(Long tenantId, Long userId, Collection<Long> conversationIds);

    ImUserTagDO requireOwnedTag(Long tenantId, Long userId, Long tagId);
}
