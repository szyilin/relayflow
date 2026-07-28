package com.relayflow.module.system.service.enterprise;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.infra.api.geo.GeoRegionApi;
import com.relayflow.module.infra.api.geo.dto.GeoRegionDTO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressRespVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressSaveReqVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseAddressStatusReqVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseProfileRespVO;
import com.relayflow.module.system.controller.admin.enterprise.vo.EnterpriseProfileUpdateReqVO;
import com.relayflow.module.system.dal.dataobject.SysTenantAddressDO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.mapper.SysTenantAddressMapper;
import com.relayflow.module.system.dal.mapper.SysTenantMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EnterpriseServiceImpl implements EnterpriseService {

    private static final String DATA_RESIDENCY_CN = "中国大陆";
    private static final Set<String> ADDRESS_TYPES = Set.of("JOB", "INTERVIEW", "STORE");
    private static final Set<String> ADDRESS_STATUSES = Set.of("ENABLED", "DISABLED");

    private final TenantService tenantService;
    private final SysTenantMapper tenantMapper;
    private final SysTenantAddressMapper addressMapper;
    private final GeoRegionApi geoRegionApi;
    private final ObjectMapper objectMapper;

    @Override
    public EnterpriseProfileRespVO getProfile() {
        return toProfile(tenantService.getTenant(TenantContextHolder.getRequiredTenantId()));
    }

    @Override
    @Transactional
    public EnterpriseProfileRespVO updateProfile(EnterpriseProfileUpdateReqVO request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new ServiceException(ErrorCodeConstants.ENTERPRISE_PROFILE_NAME_REQUIRED);
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        SysTenantDO tenant = tenantService.getTenant(tenantId);
        // code / dataResidency are immutable — never assign from request
        tenant.setName(name);
        tenant.setIndustry(blankToNull(request.getIndustry()));
        tenant.setScale(blankToNull(request.getScale()));
        tenant.setContactName(blankToNull(request.getContactName()));
        tenant.setContactEmail(blankToNull(request.getContactEmail()));
        tenant.setContactMobile(blankToNull(request.getContactMobile()));
        tenant.setContactLandline(blankToNull(request.getContactLandline()));
        tenantMapper.updateById(tenant);
        return toProfile(tenantService.getTenant(tenantId));
    }

    @Override
    public List<EnterpriseAddressRespVO> listAddresses(String keyword, List<String> types, String status) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LambdaQueryWrapper<SysTenantAddressDO> query = new LambdaQueryWrapper<SysTenantAddressDO>()
                .eq(SysTenantAddressDO::getTenantId, tenantId)
                .orderByDesc(SysTenantAddressDO::getCreateTime);

        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status.trim())) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!ADDRESS_STATUSES.contains(normalized)) {
                throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_STATUS_INVALID);
            }
            query.eq(SysTenantAddressDO::getStatus, normalized);
        }

        List<SysTenantAddressDO> rows = addressMapper.selectList(query);
        Set<String> typeFilter = normalizeTypeFilter(types);
        String kw = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;

        List<EnterpriseAddressRespVO> result = new ArrayList<>();
        for (SysTenantAddressDO row : rows) {
            EnterpriseAddressRespVO vo = toAddress(row);
            if (!typeFilter.isEmpty() && vo.getTypes().stream().noneMatch(typeFilter::contains)) {
                continue;
            }
            if (kw != null) {
                String path = vo.getRegionPath() == null ? "" : vo.getRegionPath().toLowerCase(Locale.ROOT);
                String detail = vo.getDetail() == null ? "" : vo.getDetail().toLowerCase(Locale.ROOT);
                if (!path.contains(kw) && !detail.contains(kw)) {
                    continue;
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional
    public EnterpriseAddressRespVO createAddress(EnterpriseAddressSaveReqVO request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<String> types = validateTypes(request.getTypes());
        validateRegion(request.getRegionId());

        SysTenantAddressDO row = new SysTenantAddressDO();
        row.setTenantId(tenantId);
        row.setTypes(writeTypes(types));
        row.setRegionId(request.getRegionId());
        row.setRegionPath(request.getRegionPath().trim());
        row.setDetail(request.getDetail().trim());
        row.setStatus("ENABLED");
        addressMapper.insert(row);
        return toAddress(addressMapper.selectById(row.getId()));
    }

    @Override
    @Transactional
    public EnterpriseAddressRespVO updateAddress(Long id, EnterpriseAddressSaveReqVO request) {
        SysTenantAddressDO row = requireAddress(id);
        List<String> types = validateTypes(request.getTypes());
        validateRegion(request.getRegionId());

        row.setTypes(writeTypes(types));
        row.setRegionId(request.getRegionId());
        row.setRegionPath(request.getRegionPath().trim());
        row.setDetail(request.getDetail().trim());
        addressMapper.updateById(row);
        return toAddress(addressMapper.selectById(id));
    }

    @Override
    @Transactional
    public EnterpriseAddressRespVO updateAddressStatus(Long id, EnterpriseAddressStatusReqVO request) {
        SysTenantAddressDO row = requireAddress(id);
        String status = request.getStatus() == null ? "" : request.getStatus().trim().toUpperCase(Locale.ROOT);
        if (!ADDRESS_STATUSES.contains(status)) {
            throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_STATUS_INVALID);
        }
        row.setStatus(status);
        addressMapper.updateById(row);
        return toAddress(addressMapper.selectById(id));
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        requireAddress(id);
        addressMapper.deleteById(id);
    }

    private SysTenantAddressDO requireAddress(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        SysTenantAddressDO row = addressMapper.selectById(id);
        if (row == null || !tenantId.equals(row.getTenantId())) {
            throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_NOT_FOUND);
        }
        return row;
    }

    private void validateRegion(Long regionId) {
        GeoRegionDTO region = geoRegionApi.getById(regionId);
        if (region == null) {
            throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_REGION_INVALID);
        }
    }

    private List<String> validateTypes(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_TYPES_INVALID);
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String item : raw) {
            if (!StringUtils.hasText(item)) {
                throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_TYPES_INVALID);
            }
            String type = item.trim().toUpperCase(Locale.ROOT);
            if (!ADDRESS_TYPES.contains(type)) {
                throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_TYPES_INVALID);
            }
            normalized.add(type);
        }
        if (normalized.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_TYPES_INVALID);
        }
        return new ArrayList<>(normalized);
    }

    private Set<String> normalizeTypeFilter(List<String> types) {
        if (types == null || types.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> filter = new LinkedHashSet<>();
        for (String item : types) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            for (String part : item.split(",")) {
                if (!StringUtils.hasText(part)) {
                    continue;
                }
                String type = part.trim().toUpperCase(Locale.ROOT);
                if (!ADDRESS_TYPES.contains(type)) {
                    throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_TYPES_INVALID);
                }
                filter.add(type);
            }
        }
        return filter;
    }

    private EnterpriseProfileRespVO toProfile(SysTenantDO tenant) {
        EnterpriseProfileRespVO vo = new EnterpriseProfileRespVO();
        vo.setCode(tenant.getCode());
        vo.setName(tenant.getName());
        vo.setIndustry(tenant.getIndustry());
        vo.setScale(tenant.getScale());
        vo.setDataResidency(DATA_RESIDENCY_CN);
        vo.setContactName(tenant.getContactName());
        vo.setContactEmail(tenant.getContactEmail());
        vo.setContactMobile(tenant.getContactMobile());
        vo.setContactLandline(tenant.getContactLandline());
        return vo;
    }

    private EnterpriseAddressRespVO toAddress(SysTenantAddressDO row) {
        EnterpriseAddressRespVO vo = new EnterpriseAddressRespVO();
        vo.setId(row.getId());
        vo.setTypes(readTypes(row.getTypes()));
        vo.setRegionId(row.getRegionId());
        vo.setRegionPath(row.getRegionPath());
        vo.setDetail(row.getDetail());
        vo.setStatus(row.getStatus());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    private String writeTypes(List<String> types) {
        try {
            return objectMapper.writeValueAsString(types);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.ENTERPRISE_ADDRESS_TYPES_INVALID);
        }
    }

    private List<String> readTypes(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return new ArrayList<>();
        }
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
