package com.relayflow.module.infra.controller.app.file;

import com.relayflow.module.infra.service.file.FileDownloadService;
import com.relayflow.module.infra.service.file.model.FileDownloadRedirect;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/infra/file")
public class AppFileController {

    private final FileDownloadService fileDownloadService;

    @GetMapping("/public/{fileId}")
    public ResponseEntity<Void> downloadPublicFile(@PathVariable Long fileId) {
        FileDownloadRedirect redirect = fileDownloadService.resolvePublicDownload(fileId);
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
