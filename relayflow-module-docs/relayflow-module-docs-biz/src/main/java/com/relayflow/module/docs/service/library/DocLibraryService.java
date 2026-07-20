package com.relayflow.module.docs.service.library;

import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeCreateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeSummaryRespVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryTreeRespVO;

public interface DocLibraryService {

    DocLibraryTreeRespVO getTree();

    DocLibraryNodeSummaryRespVO createNode(DocLibraryNodeCreateReqVO request);

    DocLibraryNodeSummaryRespVO updateNode(Long nodeId, DocLibraryNodeUpdateReqVO request);

    void deleteNode(Long nodeId);
}
