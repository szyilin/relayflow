package com.relayflow.module.infra.service.file.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileDownloadRedirect {

    private String url;
    private String cacheControl;
}
