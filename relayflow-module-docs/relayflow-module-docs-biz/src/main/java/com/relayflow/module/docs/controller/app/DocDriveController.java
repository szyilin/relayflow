package com.relayflow.module.docs.controller.app;

import com.relayflow.common.pojo.CommonResult;
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
import com.relayflow.module.docs.service.drive.DocDriveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/docs/drive")
public class DocDriveController {

    private final DocDriveService docDriveService;

    @GetMapping("/folders")
    public CommonResult<DocDriveFolderListRespVO> listFolders(
            @RequestParam(required = false) Long parentId) {
        return CommonResult.success(docDriveService.listFolders(parentId));
    }

    @PostMapping("/folders")
    public CommonResult<DocDriveFolderRespVO> createFolder(
            @Valid @RequestBody DocDriveFolderCreateReqVO request) {
        return CommonResult.success(docDriveService.createFolder(request));
    }

    @PutMapping("/folders/{folderId}")
    public CommonResult<DocDriveFolderRespVO> updateFolder(
            @PathVariable Long folderId,
            @RequestBody DocDriveFolderUpdateReqVO request) {
        return CommonResult.success(docDriveService.updateFolder(folderId, request));
    }

    @DeleteMapping("/folders/{folderId}")
    public CommonResult<Boolean> deleteFolder(@PathVariable Long folderId) {
        docDriveService.deleteFolder(folderId);
        return CommonResult.success(true);
    }

    @GetMapping("/items")
    public CommonResult<DocDriveListingRespVO> listItems(
            @RequestParam(required = false) Long folderId) {
        return CommonResult.success(docDriveService.listItems(folderId));
    }

    @PostMapping("/files")
    public CommonResult<DocDriveItemRespVO> registerFile(
            @Valid @RequestBody DocDriveFileRegisterReqVO request) {
        return CommonResult.success(docDriveService.registerFile(request));
    }

    @PutMapping("/items/{itemId}")
    public CommonResult<DocDriveItemRespVO> updateItem(
            @PathVariable Long itemId,
            @RequestBody DocDriveItemUpdateReqVO request) {
        return CommonResult.success(docDriveService.updateItem(itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public CommonResult<Boolean> deleteItem(@PathVariable Long itemId) {
        docDriveService.deleteItem(itemId);
        return CommonResult.success(true);
    }

    @PostMapping("/placements/move")
    public CommonResult<DocPlacementMoveRespVO> movePlacement(
            @Valid @RequestBody DocPlacementMoveReqVO request) {
        return CommonResult.success(docDriveService.movePlacement(request));
    }
}
