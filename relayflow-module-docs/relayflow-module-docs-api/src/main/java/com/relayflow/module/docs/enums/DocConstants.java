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

    private DocConstants() {
    }
}
