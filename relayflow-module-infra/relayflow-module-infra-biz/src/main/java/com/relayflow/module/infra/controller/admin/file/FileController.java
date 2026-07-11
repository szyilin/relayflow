package com.relayflow.module.infra.controller.admin.file;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.infra.controller.admin.file.vo.FileListItemRespVO;
import com.relayflow.module.infra.controller.admin.file.vo.FilePageReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmRespVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionCreateReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionRespVO;
import com.relayflow.module.infra.service.file.FileService;
import com.relayflow.module.infra.service.file.FileUploadSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final FileService fileService;

    @PreAuthorize("hasAuthority('infra:file:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<FileListItemRespVO>> getFilePage(@Valid FilePageReqVO request) {
        return CommonResult.success(fileService.getFilePage(request));
    }

    @PreAuthorize("hasAuthority('infra:file:delete')")
    @DeleteMapping("/{id}")
    public CommonResult<Boolean> deleteFile(@PathVariable Long id) {
        fileService.deleteFile(id);
        return CommonResult.success(true);
    }

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
