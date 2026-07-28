package com.relayflow.module.agent.controller.app.skill;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.agent.controller.app.skill.vo.SkillCatalogItemRespVO;
import com.relayflow.module.agent.skill.SkillDescriptor;
import com.relayflow.module.agent.skill.SkillRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/agent/skill")
public class AppAgentSkillController {

    private final SkillRegistry skillRegistry;

    @GetMapping("/catalog")
    public CommonResult<List<SkillCatalogItemRespVO>> catalog() {
        List<SkillCatalogItemRespVO> items = skillRegistry.listCatalog().stream()
                .map(this::toVo)
                .toList();
        return CommonResult.success(items);
    }

    private SkillCatalogItemRespVO toVo(SkillDescriptor descriptor) {
        SkillCatalogItemRespVO vo = new SkillCatalogItemRespVO();
        vo.setCode(descriptor.getCode());
        vo.setName(descriptor.getName());
        vo.setDescription(descriptor.getDescription());
        vo.setInputSchema(descriptor.getInputSchema());
        return vo;
    }
}
