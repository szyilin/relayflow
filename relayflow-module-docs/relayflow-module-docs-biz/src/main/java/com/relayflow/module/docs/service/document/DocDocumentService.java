package com.relayflow.module.docs.service.document;

import com.relayflow.module.docs.controller.app.vo.DocDocumentBodySaveReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDocumentBodySaveRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDocumentRespVO;
import com.relayflow.module.docs.controller.app.vo.DocExportMdRespVO;
import com.relayflow.module.docs.controller.app.vo.DocRecentItemRespVO;

import java.util.List;

public interface DocDocumentService {

    DocDocumentRespVO getDocument(Long objectId);

    DocDocumentBodySaveRespVO saveBody(Long objectId, DocDocumentBodySaveReqVO request);

    DocExportMdRespVO exportMarkdown(Long objectId);

    List<DocRecentItemRespVO> listRecent(int limit);
}
