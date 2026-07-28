package com.relayflow.module.system.api.dept;

public interface DeptApi {

    boolean existsInTenant(Long tenantId, Long deptId);

    String getDeptName(Long tenantId, Long deptId);
}
