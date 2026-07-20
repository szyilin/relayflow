package com.relayflow.module.docs.service.drive;

import com.relayflow.module.docs.controller.app.vo.DocDriveFileRegisterReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveFolderCreateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveFolderListRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveFolderRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveFolderUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveItemRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveItemUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDriveListingRespVO;
import com.relayflow.module.docs.controller.app.vo.DocPlacementMoveReqVO;
import com.relayflow.module.docs.controller.app.vo.DocPlacementMoveRespVO;

public interface DocDriveService {

    DocDriveFolderListRespVO listFolders(Long parentId);

    DocDriveListingRespVO listItems(Long folderId);

    DocDriveFolderRespVO createFolder(DocDriveFolderCreateReqVO request);

    DocDriveFolderRespVO updateFolder(Long folderId, DocDriveFolderUpdateReqVO request);

    void deleteFolder(Long folderId);

    DocDriveItemRespVO registerFile(DocDriveFileRegisterReqVO request);

    DocDriveItemRespVO updateItem(Long itemId, DocDriveItemUpdateReqVO request);

    void deleteItem(Long itemId);

    DocPlacementMoveRespVO movePlacement(DocPlacementMoveReqVO request);
}
