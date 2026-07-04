package com.relayflow.module.system.service.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mysql.SysTenantUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserMapper;
import com.relayflow.module.system.enums.TenantUserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TenantProperties tenantProperties;

    public UserServiceImpl(SysUserMapper userMapper,
                           SysTenantUserMapper tenantUserMapper,
                           PasswordEncoder passwordEncoder,
                           TenantProperties tenantProperties) {
        this.userMapper = userMapper;
        this.tenantUserMapper = tenantUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tenantProperties = tenantProperties;
    }

    @Override
    @Transactional
    public Long createUser(UserCreateReqDTO request) {
        SysUserDO user = new SysUserDO();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setMobile(request.getMobile());
        user.setEmail(request.getEmail());
        userMapper.insert(user);

        Long tenantId = resolveTenantId();
        SysTenantUserDO tenantUser = new SysTenantUserDO();
        tenantUser.setTenantId(tenantId);
        tenantUser.setUserId(user.getId());
        tenantUser.setStatus(TenantUserStatus.ACTIVE);
        tenantUserMapper.insert(tenantUser);
        return user.getId();
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }
}
