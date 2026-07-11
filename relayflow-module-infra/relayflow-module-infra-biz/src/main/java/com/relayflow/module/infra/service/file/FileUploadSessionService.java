package com.relayflow.module.infra.service.file;

import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmRespVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionCreateReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionRespVO;

public interface FileUploadSessionService {

    FileUploadSessionRespVO createSession(FileUploadSessionCreateReqVO request);

    FileUploadConfirmRespVO confirm(FileUploadConfirmReqVO request);
}
