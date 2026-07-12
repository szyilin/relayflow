package com.relayflow.module.infra.controller.app.file;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmRespVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionCreateReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionRespVO;
import com.relayflow.module.infra.service.file.FileDownloadService;
import com.relayflow.module.infra.service.file.FileUploadSessionService;
import com.relayflow.module.infra.service.file.model.FileDownloadRedirect;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/infra/file")
public class AppFileController {

    private final FileDownloadService fileDownloadService;
    private final FileUploadSessionService fileUploadSessionService;

    @GetMapping("/public/{fileId}")
    public ResponseEntity<Void> downloadPublicFile(@PathVariable Long fileId) {
        FileDownloadRedirect redirect = fileDownloadService.resolvePublicDownload(fileId);
        return buildRedirect(redirect);
    }

    @PostMapping("/upload-session")
    public CommonResult<FileUploadSessionRespVO> createUploadSession(
            @Valid @RequestBody FileUploadSessionCreateReqVO request) {
        return CommonResult.success(fileUploadSessionService.createSession(request));
    }

    @PostMapping("/upload-confirm")
    public CommonResult<FileUploadConfirmRespVO> confirmUpload(
            @Valid @RequestBody FileUploadConfirmReqVO request) {
        return CommonResult.success(fileUploadSessionService.confirm(request));
    }

    @GetMapping("/download")
    public ResponseEntity<Void> downloadMemberFile(@RequestParam Long fileId) {
        FileDownloadRedirect redirect = fileDownloadService.resolveMemberDownload(fileId);
        return buildRedirect(redirect);
    }

    public static ResponseEntity<Void> buildRedirect(FileDownloadRedirect redirect) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect.getUrl()));
        if (redirect.getCacheControl() != null) {
            builder.header(HttpHeaders.CACHE_CONTROL, redirect.getCacheControl());
        }
        return builder.build();
    }
}
