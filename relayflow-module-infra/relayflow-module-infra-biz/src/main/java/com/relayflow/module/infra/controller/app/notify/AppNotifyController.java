package com.relayflow.module.infra.controller.app.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.infra.controller.app.notify.vo.NotifyItemRespVO;
import com.relayflow.module.infra.controller.app.notify.vo.NotifyPageReqVO;
import com.relayflow.module.infra.controller.app.notify.vo.NotifyReadAllReqVO;
import com.relayflow.module.infra.controller.app.notify.vo.NotifyReadReqVO;
import com.relayflow.module.infra.controller.app.notify.vo.NotifyUnreadCountRespVO;
import com.relayflow.module.infra.convert.NotifyConvert;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import com.relayflow.module.infra.service.notify.NotifyInboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/app-api/infra/notify")
public class AppNotifyController {

    private final NotifyInboxService notifyInboxService;
    private final ObjectMapper objectMapper;

    @GetMapping("/page")
    public CommonResult<PageResult<NotifyItemRespVO>> page(@Valid NotifyPageReqVO request) {
        Long userId = requireLoginUserId();
        PageResult<InfraNotifyDO> page = notifyInboxService.pageByUserId(
                userId, request.getType(), request.getPageNo(), request.getPageSize());
        PageResult<NotifyItemRespVO> response = PageResult.of(
                NotifyConvert.toRespList(page.getList(), objectMapper),
                page.getTotal());
        return CommonResult.success(response);
    }

    @GetMapping("/unread-count")
    public CommonResult<NotifyUnreadCountRespVO> unreadCount() {
        Long userId = requireLoginUserId();
        NotifyUnreadCountRespVO response = new NotifyUnreadCountRespVO();
        response.setUnreadCount(notifyInboxService.countUnreadByUserId(userId));
        Map<String, Long> byType = notifyInboxService.countUnreadGroupByType(userId);
        if (!CollectionUtils.isEmpty(byType)) {
            response.setByType(byType);
        }
        return CommonResult.success(response);
    }

    @PostMapping("/read")
    public CommonResult<Boolean> markRead(@Valid @RequestBody NotifyReadReqVO request) {
        Long userId = requireLoginUserId();
        notifyInboxService.markReadByIds(userId, request.getIds());
        return CommonResult.success(true);
    }

    @PostMapping("/read-all")
    public CommonResult<Boolean> markAllRead(@RequestBody(required = false) NotifyReadAllReqVO request) {
        Long userId = requireLoginUserId();
        String type = request != null ? request.getType() : null;
        notifyInboxService.markAllReadByUserId(userId, type);
        return CommonResult.success(true);
    }

    private Long requireLoginUserId() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.NOTIFY_LOGIN_REQUIRED);
        }
        return loginUser.getUserId();
    }
}
