package com.relayflow.module.im.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.PresenceBatchRespVO;
import com.relayflow.module.im.service.presence.ImPresenceService;
import com.relayflow.module.system.enums.ErrorCodeConstants;
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
        LoginUser loginUser = requireLoginUser();
        List<Long> ids = parseUserIds(userIds);
        return CommonResult.success(presenceService.batchPresence(loginUser.getTenantId(), ids));
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

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
