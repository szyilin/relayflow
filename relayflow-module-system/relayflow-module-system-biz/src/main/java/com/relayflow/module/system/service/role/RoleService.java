package com.relayflow.module.system.service.role;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.system.controller.admin.role.vo.RoleCreateReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RolePageReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleRespVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleUpdateReqVO;

public interface RoleService {

    PageResult<RoleRespVO> getRolePage(RolePageReqVO request);

    RoleRespVO getRole(Long id);

    Long createRole(RoleCreateReqVO request);

    void updateRole(RoleUpdateReqVO request);

    void deleteRole(Long id);
}
