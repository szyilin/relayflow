package com.relayflow.module.system.controller.app.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MemberInvitePendingListRespVO {

    private List<MemberInvitePendingItemVO> items = new ArrayList<>();
}
