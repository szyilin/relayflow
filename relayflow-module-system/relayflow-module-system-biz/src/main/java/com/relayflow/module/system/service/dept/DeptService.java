package com.relayflow.module.system.service.dept;

import com.relayflow.module.system.controller.admin.dept.vo.DeptCreateReqVO;
import com.relayflow.module.system.controller.admin.dept.vo.DeptRespVO;
import com.relayflow.module.system.controller.admin.dept.vo.DeptUpdateReqVO;

import java.util.List;

public interface DeptService {

    List<DeptRespVO> getDeptList();

    DeptRespVO getDept(Long id);

    Long createDept(DeptCreateReqVO request);

    void updateDept(DeptUpdateReqVO request);

    void deleteDept(Long id);

    Long getOrCreateRootDept(Long tenantId);
}
