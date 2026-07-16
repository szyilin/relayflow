package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.CardActionReqVO;
import com.relayflow.module.im.controller.app.vo.CardActionRespVO;
import com.relayflow.module.im.service.card.CardActionIngress;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/im/card")
public class ImCardController {

    private final CardActionIngress cardActionIngress;

    @PostMapping("/action")
    public CommonResult<CardActionRespVO> action(@Valid @RequestBody CardActionReqVO request) {
        return CommonResult.success(cardActionIngress.handle(request));
    }
}
