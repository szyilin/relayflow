package com.relayflow.module.docs.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    DOC_NOT_FOUND(1_006_001_001, "文档不存在"),
    DOC_FORBIDDEN(1_006_001_002, "无权操作该文档"),
    DOC_VERSION_CONFLICT(1_006_001_003, "正文版本冲突，请刷新后重试"),
    DOC_PARENT_INVALID(1_006_001_004, "父节点无效或会形成循环"),
    DOC_TYPE_UNSUPPORTED(1_006_001_005, "不支持的文档类型"),
    DOC_EXPORT_FORMAT_UNSUPPORTED(1_006_001_006, "暂不支持该导出格式"),
    DOC_DRIVE_FOLDER_NOT_EMPTY(1_006_001_007, "文件夹非空，请先移走或删除内容"),
    DOC_DRIVE_FOLDER_INVALID(1_006_001_008, "云盘文件夹无效或会形成循环"),
    DOC_STORAGE_FILE_INVALID(1_006_001_009, "存储文件无效或不属于当前用户"),
    DOC_PLACEMENT_INVALID(1_006_001_010, "文档放置无效或不支持该移动");

    private final Integer code;
    private final String msg;
}
