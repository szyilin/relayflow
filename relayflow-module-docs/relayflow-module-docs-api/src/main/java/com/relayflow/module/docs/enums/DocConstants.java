package com.relayflow.module.docs.enums;

public final class DocConstants {

    public static final String OBJECT_TYPE_RICH_DOC = "RICH_DOC";
    public static final String OBJECT_TYPE_FILE = "FILE";
    public static final String BODY_FORMAT_TIPTAP_JSON_V1 = "tiptap_json_v1";
    public static final String DEFAULT_TITLE = "未命名文档";
    public static final String DEFAULT_FOLDER_NAME = "新建文件夹";
    public static final String DEFAULT_BODY_JSON =
            "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}";
    /** infra_file_binding.biz_type for Drive FILE objects */
    public static final String FILE_BIND_BIZ_TYPE = "doc_object";
    public static final String PLACEMENT_TARGET_DRIVE = "DRIVE";
    public static final String PLACEMENT_TARGET_LIBRARY = "LIBRARY";

    public static final String SHARE_PERMISSION_VIEW = "VIEW";
    public static final String SHARE_PERMISSION_EDIT = "EDIT";

    public static final String ACCESS_STATE_OK = "OK";
    public static final String ACCESS_STATE_DELETED = "DELETED";
    public static final String ACCESS_STATE_NO_ACCESS = "NO_ACCESS";

    public static final String VISIT_SOURCE_OWNER = "OWNER";
    public static final String VISIT_SOURCE_SHARED = "SHARED";

    public static final String LOCATION_LIBRARY = "我的文档库";
    public static final String LOCATION_DRIVE = "云盘";
    public static final String LOCATION_SHARED = "与我共享";
    public static final String LOCATION_DELETED = "已删除";

    private DocConstants() {
    }
}
