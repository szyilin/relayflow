package com.relayflow.module.system.service.role;

import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.system.controller.admin.role.vo.RoleAdminSimpleVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleCreateReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleMemberRemoveReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleMembersAddReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RolePageReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleRespVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleUpdateReqVO;

import java.util.List;

public interface RoleService {

    PageResult<RoleRespVO> getRolePage(RolePageReqVO request);

    RoleRespVO getRole(Long id);

    Long createRole(RoleCreateReqVO request);

    void updateRole(RoleUpdateReqVO request);

    void deleteRole(Long id);

    List<RoleAdminSimpleVO> listMembers(Long roleId);

    void addMembers(RoleMembersAddReqVO request);

    void removeMember(RoleMemberRemoveReqVO request);
}
