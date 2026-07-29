package com.relayflow.module.docs.service.admin;

import com.relayflow.module.docs.controller.admin.vo.DocAdminFilePageReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminFilePageRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminObjectIdsReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminRestoreReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminTransferOwnerReqVO;

public interface DocAdminFilesService {

    DocAdminFilePageRespVO page(DocAdminFilePageReqVO request);

    void transferOwner(DocAdminTransferOwnerReqVO request);

    void delete(DocAdminObjectIdsReqVO request);

    void restore(DocAdminRestoreReqVO request);

    void purge(DocAdminObjectIdsReqVO request);

    /** Backfill denormalized FILE size_bytes; returns updated row count. */
    int backfillSizeBytes();
}
