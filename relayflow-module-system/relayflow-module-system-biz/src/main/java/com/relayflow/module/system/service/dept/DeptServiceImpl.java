package com.relayflow.module.system.service.dept;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.controller.admin.dept.vo.DeptCreateReqVO;
import com.relayflow.module.system.controller.admin.dept.vo.DeptRespVO;
import com.relayflow.module.system.controller.admin.dept.vo.DeptUpdateReqVO;
import com.relayflow.module.system.convert.DeptConvert;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.mapper.SysDeptMapper;
import com.relayflow.module.system.dal.mapper.SysUserDeptMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final TenantProperties tenantProperties;
    private final TenantService tenantService;

    @Override
    public List<DeptRespVO> getDeptList() {
        Long tenantId = resolveTenantId();
        List<SysDeptDO> depts = deptMapper.selectList(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getTenantId, tenantId)
                .orderByAsc(SysDeptDO::getSort)
                .orderByAsc(SysDeptDO::getId));
        return depts.stream().map(DeptConvert.INSTANCE::toVo).toList();
    }

    @Override
    public List<DeptRespVO> getEnabledDeptList() {
        Long tenantId = resolveTenantId();
        List<SysDeptDO> depts = deptMapper.selectList(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getTenantId, tenantId)
                .eq(SysDeptDO::getStatus, 0)
                .orderByAsc(SysDeptDO::getSort)
                .orderByAsc(SysDeptDO::getId));
        return depts.stream().map(DeptConvert.INSTANCE::toVo).toList();
    }

    @Override
    public DeptRespVO getDept(Long id) {
        return DeptConvert.INSTANCE.toVo(requireDept(id, resolveTenantId()));
    }

    @Override
    @Transactional
    public Long createDept(DeptCreateReqVO request) {
        Long tenantId = resolveTenantId();
        validateParentExists(request.getParentId(), tenantId, null);

        SysDeptDO dept = new SysDeptDO();
        dept.setTenantId(tenantId);
        dept.setParentId(request.getParentId());
        dept.setName(request.getName().trim());
        dept.setSort(request.getSort() != null ? request.getSort() : 0);
        dept.setLeaderUserId(request.getLeaderUserId());
        dept.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    @Transactional
    public void updateDept(DeptUpdateReqVO request) {
        Long tenantId = resolveTenantId();
        SysDeptDO existing = requireDept(request.getId(), tenantId);
        validateParentExists(request.getParentId(), tenantId, request.getId());

        existing.setParentId(request.getParentId());
        existing.setName(request.getName().trim());
        if (request.getSort() != null) {
            existing.setSort(request.getSort());
        }
        existing.setLeaderUserId(request.getLeaderUserId());
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        deptMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteDept(Long id) {
        Long tenantId = resolveTenantId();
        SysDeptDO dept = requireDept(id, tenantId);
        if (dept.getParentId() == null || dept.getParentId() == 0L) {
            throw new ServiceException(ErrorCodeConstants.DEPT_ROOT_DELETE_FORBIDDEN);
        }

        Long childCount = deptMapper.selectCount(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getTenantId, tenantId)
                .eq(SysDeptDO::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new ServiceException(ErrorCodeConstants.DEPT_HAS_CHILDREN);
        }

        Long userCount = userDeptMapper.selectCount(Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getTenantId, tenantId)
                .eq(SysUserDeptDO::getDeptId, id));
        if (userCount != null && userCount > 0) {
            throw new ServiceException(ErrorCodeConstants.DEPT_HAS_USERS);
        }

        deptMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Long getOrCreateRootDept(Long tenantId) {
        SysDeptDO root = deptMapper.selectOne(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getTenantId, tenantId)
                .eq(SysDeptDO::getParentId, 0L)
                .orderByAsc(SysDeptDO::getId)
                .last("LIMIT 1"));
        if (root != null) {
            return root.getId();
        }

        String tenantName = tenantService.getTenant(tenantId).getName();
        SysDeptDO dept = new SysDeptDO();
        dept.setTenantId(tenantId);
        dept.setParentId(0L);
        dept.setName(tenantName);
        dept.setSort(0);
        dept.setStatus(0);
        deptMapper.insert(dept);
        return dept.getId();
    }

    private SysDeptDO requireDept(Long id, Long tenantId) {
        SysDeptDO dept = deptMapper.selectOne(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getId, id)
                .eq(SysDeptDO::getTenantId, tenantId));
        if (dept == null) {
            throw new ServiceException(ErrorCodeConstants.DEPT_NOT_FOUND);
        }
        return dept;
    }

    private void validateParentExists(Long parentId, Long tenantId, Long currentDeptId) {
        if (parentId == null || parentId == 0L) {
            return;
        }

        SysDeptDO parent = deptMapper.selectOne(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getId, parentId)
                .eq(SysDeptDO::getTenantId, tenantId));
        if (parent == null) {
            throw new ServiceException(ErrorCodeConstants.DEPT_PARENT_NOT_FOUND);
        }

        if (currentDeptId != null) {
            if (Objects.equals(currentDeptId, parentId)) {
                throw new ServiceException(ErrorCodeConstants.DEPT_PARENT_INVALID);
            }
            Map<Long, List<Long>> childrenByParent = loadDeptChildrenMap(tenantId);
            Set<Long> descendants = collectDeptAndChildren(currentDeptId, childrenByParent);
            if (descendants.contains(parentId)) {
                throw new ServiceException(ErrorCodeConstants.DEPT_PARENT_INVALID);
            }
        }
    }

    private Map<Long, List<Long>> loadDeptChildrenMap(Long tenantId) {
        List<SysDeptDO> depts = deptMapper.selectList(Wrappers.<SysDeptDO>lambdaQuery()
                .eq(SysDeptDO::getTenantId, tenantId));
        Map<Long, List<Long>> childrenByParent = new HashMap<>();
        for (SysDeptDO dept : depts) {
            Long parentId = dept.getParentId() != null ? dept.getParentId() : 0L;
            childrenByParent.computeIfAbsent(parentId, ignored -> new java.util.ArrayList<>()).add(dept.getId());
        }
        return childrenByParent;
    }

    private Set<Long> collectDeptAndChildren(Long rootDeptId, Map<Long, List<Long>> childrenByParent) {
        Set<Long> deptIds = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootDeptId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (!deptIds.add(current)) {
                continue;
            }
            queue.addAll(childrenByParent.getOrDefault(current, List.of()));
        }
        return deptIds;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }
}
