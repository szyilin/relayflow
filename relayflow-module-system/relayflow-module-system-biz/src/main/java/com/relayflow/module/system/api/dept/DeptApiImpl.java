package com.relayflow.module.system.api.dept;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import com.relayflow.module.system.dal.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeptApiImpl implements DeptApi {

    private final SysDeptMapper deptMapper;

    @Override
    public boolean existsInTenant(Long tenantId, Long deptId) {
        if (tenantId == null || deptId == null || deptId <= 0) {
            return false;
        }
        Long count = deptMapper.selectCount(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getTenantId, tenantId)
                .eq(SysDeptDO::getId, deptId));
        return count != null && count > 0;
    }

    @Override
    public String getDeptName(Long tenantId, Long deptId) {
        if (tenantId == null || deptId == null || deptId <= 0) {
            return null;
        }
        SysDeptDO dept = deptMapper.selectOne(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getTenantId, tenantId)
                .eq(SysDeptDO::getId, deptId)
                .last("LIMIT 1"));
        return dept != null ? dept.getName() : null;
    }
}
