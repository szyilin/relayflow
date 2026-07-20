package com.relayflow.module.docs.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.docs.controller.app.vo.DocDocumentBodySaveReqVO;
import com.relayflow.module.docs.controller.app.vo.DocDocumentBodySaveRespVO;
import com.relayflow.module.docs.controller.app.vo.DocDocumentRespVO;
import com.relayflow.module.docs.controller.app.vo.DocExportMdRespVO;
import com.relayflow.module.docs.controller.app.vo.DocRecentItemRespVO;
import com.relayflow.module.docs.enums.ErrorCodeConstants;
import com.relayflow.module.docs.service.document.DocDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/docs")
public class DocDocumentController {

    private final DocDocumentService docDocumentService;

    @GetMapping("/documents/{objectId}")
    public CommonResult<DocDocumentRespVO> getDocument(@PathVariable Long objectId) {
        return CommonResult.success(docDocumentService.getDocument(objectId));
    }

    @PutMapping("/documents/{objectId}/body")
    public CommonResult<DocDocumentBodySaveRespVO> saveBody(
            @PathVariable Long objectId,
            @Valid @RequestBody DocDocumentBodySaveReqVO request) {
        return CommonResult.success(docDocumentService.saveBody(objectId, request));
    }

    @GetMapping("/documents/{objectId}/export")
    public CommonResult<DocExportMdRespVO> exportDocument(
            @PathVariable Long objectId,
            @RequestParam(value = "format", defaultValue = "md") String format) {
        if (!"md".equalsIgnoreCase(format)) {
            throw new ServiceException(ErrorCodeConstants.DOC_EXPORT_FORMAT_UNSUPPORTED);
        }
        return CommonResult.success(docDocumentService.exportMarkdown(objectId));
    }

    @GetMapping("/recent")
    public CommonResult<List<DocRecentItemRespVO>> recent(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return CommonResult.success(docDocumentService.listRecent(limit));
    }
}
