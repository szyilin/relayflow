package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.util.List;

@Data
public class DocDriveListingRespVO {

    private List<DocDriveFolderRespVO> folders;
    private List<DocDriveItemRespVO> items;
}
