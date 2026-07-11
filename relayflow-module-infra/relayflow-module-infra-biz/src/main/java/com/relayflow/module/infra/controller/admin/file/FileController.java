package com.relayflow.module.infra.controller.admin.file;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmRespVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionCreateReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionRespVO;
import com.relayflow.module.infra.service.file.FileUploadSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/infra/file")
public class FileController {

    private final FileUploadSessionService fileUploadSessionService;

    @PreAuthorize("hasAuthority('infra:file:upload')")
    @PostMapping("/upload-session")
    public CommonResult<FileUploadSessionRespVO> createUploadSession(
            @Valid @RequestBody FileUploadSessionCreateReqVO request) {
        return CommonResult.success(fileUploadSessionService.createSession(request));
    }

    @PreAuthorize("hasAuthority('infra:file:upload')")
    @PostMapping("/confirm")
    public CommonResult<FileUploadConfirmRespVO> confirmUpload(
            @Valid @RequestBody FileUploadConfirmReqVO request) {
        return CommonResult.success(fileUploadSessionService.confirm(request));
    }
}
