package com.relayflow.module.system.service.enterprise;

import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressRespVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressSaveReqVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressStatusReqVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseProfileRespVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseProfileUpdateReqVO;

import java.util.List;

public interface EnterpriseService {

    EnterpriseProfileRespVO getProfile();

    EnterpriseProfileRespVO updateProfile(EnterpriseProfileUpdateReqVO request);

    List<EnterpriseAddressRespVO> listAddresses(String keyword, List<String> types, String status);

    EnterpriseAddressRespVO createAddress(EnterpriseAddressSaveReqVO request);

    EnterpriseAddressRespVO updateAddress(Long id, EnterpriseAddressSaveReqVO request);

    EnterpriseAddressRespVO updateAddressStatus(Long id, EnterpriseAddressStatusReqVO request);

    void deleteAddress(Long id);
}
