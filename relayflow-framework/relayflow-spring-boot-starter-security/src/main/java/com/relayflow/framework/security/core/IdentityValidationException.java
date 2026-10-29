package com.relayflow.framework.security.core;

import com.relayflow.common.exception.GlobalErrorCodeConstants;
import com.relayflow.common.exception.ServiceException;
import lombok.Getter;

@Getter
public class IdentityValidationException extends ServiceException {

    private final int httpStatus;

    private IdentityValidationException(int httpStatus, GlobalErrorCodeConstants error) {
        super(error);
        this.httpStatus = httpStatus;
    }

    public static IdentityValidationException unauthorized() {
        return new IdentityValidationException(401, GlobalErrorCodeConstants.UNAUTHORIZED);
    }

    public static IdentityValidationException inactiveMember() {
        return new IdentityValidationException(403, GlobalErrorCodeConstants.MEMBERSHIP_INACTIVE);
    }

    public static IdentityValidationException unavailable() {
        return new IdentityValidationException(503, GlobalErrorCodeConstants.IDENTITY_STATE_UNAVAILABLE);
    }
}
