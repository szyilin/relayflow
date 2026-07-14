package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.PresenceBatchRespVO;
import com.relayflow.module.im.service.presence.ImPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/im/presence")
public class ImPresenceController {

    private final ImPresenceService presenceService;

    @GetMapping("/batch")
    public CommonResult<PresenceBatchRespVO> batchPresence(@RequestParam("userIds") String userIds) {
        return CommonResult.success(presenceService.batchPresence(parseUserIds(userIds)));
    }

    private List<Long> parseUserIds(String userIds) {
        if (!StringUtils.hasText(userIds)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : userIds.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip invalid ids
            }
        }
        return ids;
    }
}
