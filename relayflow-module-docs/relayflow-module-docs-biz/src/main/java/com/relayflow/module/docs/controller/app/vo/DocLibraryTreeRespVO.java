package com.relayflow.module.docs.controller.app.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DocLibraryTreeRespVO {

    private List<DocLibraryNodeTreeVO> nodes = new ArrayList<>();
}
