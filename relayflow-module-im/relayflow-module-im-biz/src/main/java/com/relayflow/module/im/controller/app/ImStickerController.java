package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.StickerPackRespVO;
import com.relayflow.module.im.service.sticker.ImStickerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/im/sticker")
public class ImStickerController {

    private final ImStickerService stickerService;

    @GetMapping("/packs")
    public CommonResult<List<StickerPackRespVO>> listPacks() {
        return CommonResult.success(stickerService.listVisiblePacks());
    }
}
