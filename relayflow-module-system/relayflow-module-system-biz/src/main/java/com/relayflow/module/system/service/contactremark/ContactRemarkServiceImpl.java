package com.relayflow.module.system.service.contactremark;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.app.vo.AppContactRemarkRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactRemarkUpdateReqVO;
import com.relayflow.module.system.dal.dataobject.SysContactRemarkDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.mapper.SysContactRemarkMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ContactRemarkServiceImpl implements ContactRemarkService {

    private final SysContactRemarkMapper contactRemarkMapper;
    private final SysTenantUserMapper tenantUserMapper;

    @Override
    public AppContactRemarkRespVO getMyRemark(Long targetUserId) {
        Long ownerUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, ownerUserId);
        requireActiveMember(tenantId, targetUserId);
        SysContactRemarkDO row = findRow(tenantId, ownerUserId, targetUserId);
        return toResp(targetUserId, row);
    }

    @Override
    @Transactional
    public AppContactRemarkRespVO updateMyRemark(Long targetUserId, AppContactRemarkUpdateReqVO request) {
        Long ownerUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, ownerUserId);
        requireActiveMember(tenantId, targetUserId);

        String remarkName = request.getRemarkName() == null ? "" : request.getRemarkName().trim();
        String description = request.getDescription() == null ? "" : request.getDescription().trim();

        SysContactRemarkDO row = findRow(tenantId, ownerUserId, targetUserId);
        if (row == null) {
            row = new SysContactRemarkDO();
            row.setTenantId(tenantId);
            row.setOwnerUserId(ownerUserId);
            row.setTargetUserId(targetUserId);
            row.setRemarkName(remarkName);
            row.setDescription(description);
            contactRemarkMapper.insert(row);
        } else {
            row.setRemarkName(remarkName);
            row.setDescription(description);
            contactRemarkMapper.updateById(row);
        }
        return toResp(targetUserId, row);
    }

    private SysContactRemarkDO findRow(Long tenantId, Long ownerUserId, Long targetUserId) {
        return contactRemarkMapper.selectOne(new LambdaQueryWrapper<SysContactRemarkDO>()
                .eq(SysContactRemarkDO::getTenantId, tenantId)
                .eq(SysContactRemarkDO::getOwnerUserId, ownerUserId)
                .eq(SysContactRemarkDO::getTargetUserId, targetUserId)
                .last("LIMIT 1"));
    }

    private void requireActiveMember(Long tenantId, Long userId) {
        SysTenantUserDO tenantUser = tenantUserMapper.selectOne(new LambdaQueryWrapper<SysTenantUserDO>()
                .eq(SysTenantUserDO::getTenantId, tenantId)
                .eq(SysTenantUserDO::getUserId, userId)
                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE)
                .last("LIMIT 1"));
        if (tenantUser == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
    }

    private static AppContactRemarkRespVO toResp(Long targetUserId, SysContactRemarkDO row) {
        AppContactRemarkRespVO resp = new AppContactRemarkRespVO();
        resp.setTargetUserId(targetUserId);
        if (row == null) {
            resp.setRemarkName("");
            resp.setDescription("");
            return resp;
        }
        resp.setRemarkName(StringUtils.hasText(row.getRemarkName()) ? row.getRemarkName() : "");
        resp.setDescription(StringUtils.hasText(row.getDescription()) ? row.getDescription() : "");
        return resp;
    }
}
