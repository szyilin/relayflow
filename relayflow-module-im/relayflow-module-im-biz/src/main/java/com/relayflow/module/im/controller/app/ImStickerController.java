package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.AddUserStickerFromMessageReqVO;
import com.relayflow.module.im.controller.app.vo.AddUserStickerReqVO;
import com.relayflow.module.im.controller.app.vo.DeleteUserStickersReqVO;
import com.relayflow.module.im.controller.app.vo.ReorderUserStickersReqVO;
import com.relayflow.module.im.controller.app.vo.StickerPackRespVO;
import com.relayflow.module.im.controller.app.vo.UserStickerItemRespVO;
import com.relayflow.module.im.service.sticker.ImStickerService;
import com.relayflow.module.im.service.sticker.ImUserStickerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/im/sticker")
public class ImStickerController {

    private final ImStickerService stickerService;
    private final ImUserStickerService userStickerService;

    @GetMapping("/packs")
    public CommonResult<List<StickerPackRespVO>> listPacks() {
        return CommonResult.success(stickerService.listVisiblePacks());
    }

    @GetMapping("/mine")
    public CommonResult<List<UserStickerItemRespVO>> listMine() {
        return CommonResult.success(userStickerService.listMine());
    }

    @PostMapping("/mine")
    public CommonResult<UserStickerItemRespVO> addMine(@Valid @RequestBody AddUserStickerReqVO request) {
        return CommonResult.success(userStickerService.add(request));
    }

    @PostMapping("/mine/from-message")
    public CommonResult<UserStickerItemRespVO> addFromMessage(
            @Valid @RequestBody AddUserStickerFromMessageReqVO request) {
        return CommonResult.success(userStickerService.addFromMessage(request));
    }

    @PutMapping("/mine/reorder")
    public CommonResult<Boolean> reorderMine(@Valid @RequestBody ReorderUserStickersReqVO request) {
        userStickerService.reorder(request);
        return CommonResult.success(true);
    }

    @PostMapping("/mine/delete")
    public CommonResult<Boolean> deleteMine(@Valid @RequestBody DeleteUserStickersReqVO request) {
        userStickerService.deleteBatch(request);
        return CommonResult.success(true);
    }
}
