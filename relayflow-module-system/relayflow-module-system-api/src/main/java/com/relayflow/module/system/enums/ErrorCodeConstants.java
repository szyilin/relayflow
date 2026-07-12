package com.relayflow.module.system.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    AUTH_LOGIN_BAD_CREDENTIALS(1_001_001_001, "用户名或密码错误"),
    AUTH_LOGIN_USER_DISABLED(1_001_001_002, "账号不可用"),
    USER_NOT_FOUND(1_001_002_001, "用户不存在"),
    USER_USERNAME_EXISTS(1_001_002_002, "用户名已存在"),
    USER_DEPT_REQUIRED(1_001_002_003, "主部门不能为空"),
    TENANT_NOT_FOUND(1_001_003_001, "租户不存在"),
    DEPT_NOT_FOUND(1_001_004_001, "部门不存在"),
    DEPT_PARENT_NOT_FOUND(1_001_004_002, "上级部门不存在"),
    DEPT_PARENT_INVALID(1_001_004_003, "上级部门不能为自身或下级部门"),
    DEPT_HAS_CHILDREN(1_001_004_004, "存在子部门，无法删除"),
    DEPT_HAS_USERS(1_001_004_005, "部门下存在用户，无法删除"),
    DEPT_ROOT_DELETE_FORBIDDEN(1_001_004_006, "根部门不可删除"),
    ROLE_NOT_FOUND(1_001_005_001, "角色不存在"),
    ROLE_CODE_DUPLICATE(1_001_005_002, "角色编码已存在"),
    ROLE_SYSTEM_DELETE_FORBIDDEN(1_001_005_003, "系统内置角色不可删除"),
    ROLE_PARENT_NOT_FOUND(1_001_005_004, "上级角色不存在"),
    ROLE_PARENT_INVALID(1_001_005_005, "上级角色不能为自身或下级角色"),
    ROLE_PERMISSION_EXCEED_PARENT(1_001_005_006, "角色权限不能超过上级角色"),
    ROLE_EXISTS_CHILDREN(1_001_005_007, "存在子角色，无法删除"),
    ROLE_EXISTS_USER(1_001_005_008, "角色已分配用户，无法删除"),
    ROLE_SYSTEM_UPDATE_FORBIDDEN(1_001_005_009, "系统内置角色不可修改");

    private final Integer code;
    private final String msg;
}
