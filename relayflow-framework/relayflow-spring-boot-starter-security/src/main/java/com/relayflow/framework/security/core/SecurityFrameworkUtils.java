package com.relayflow.framework.security.core;

import com.relayflow.common.exception.GlobalErrorCodeConstants;
import com.relayflow.common.exception.ServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityFrameworkUtils {

    private SecurityFrameworkUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /**
     * 获取当前登录用户；未登录时抛出 {@link GlobalErrorCodeConstants#UNAUTHORIZED}。
     * <p>业务代码（优先 Service）应使用本方法，禁止在 Controller 内复制 null 检查。
     */
    public static LoginUser requireLoginUser() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(GlobalErrorCodeConstants.UNAUTHORIZED);
        }
        return loginUser;
    }

    public static Long getLoginUserId() {
        LoginUser loginUser = getLoginUser();
        return loginUser != null ? loginUser.getUserId() : null;
    }

    public static Long requireLoginUserId() {
        return requireLoginUser().getUserId();
    }

    public static Long requireLoginTenantId() {
        return requireLoginUser().getTenantId();
    }
}
