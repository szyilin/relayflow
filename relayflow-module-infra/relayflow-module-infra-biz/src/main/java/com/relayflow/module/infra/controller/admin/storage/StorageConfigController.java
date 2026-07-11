package com.relayflow.module.infra.controller.admin.storage;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageConfigRespVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageEffectiveSourceReqVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageProviderSaveReqVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageTestConnectionReqVO;
import com.relayflow.module.infra.service.storage.StorageProviderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/infra/storage")
public class StorageConfigController {

    private final StorageProviderService storageProviderService;

    @PreAuthorize("hasAuthority('infra:storage:query')")
    @GetMapping("/config")
    public CommonResult<StorageConfigRespVO> getConfig() {
        return CommonResult.success(storageProviderService.getConfig());
    }

    @PreAuthorize("hasAuthority('infra:storage:update')")
    @PutMapping("/config")
    public CommonResult<Boolean> saveConfig(@Valid @RequestBody StorageProviderSaveReqVO request) {
        storageProviderService.saveConfig(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('infra:storage:update')")
    @PutMapping("/effective-source")
    public CommonResult<Boolean> setEffectiveSource(@Valid @RequestBody StorageEffectiveSourceReqVO request) {
        storageProviderService.setEffectiveSource(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('infra:storage:update')")
    @DeleteMapping("/config")
    public CommonResult<Boolean> deleteConfig(@RequestParam @NotBlank String provider) {
        storageProviderService.deleteConfig(provider);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('infra:storage:test')")
    @PostMapping("/test-connection")
    public CommonResult<Boolean> testConnection(@Valid @RequestBody StorageTestConnectionReqVO request) {
        storageProviderService.testConnection(request);
        return CommonResult.success(true);
    }
}
