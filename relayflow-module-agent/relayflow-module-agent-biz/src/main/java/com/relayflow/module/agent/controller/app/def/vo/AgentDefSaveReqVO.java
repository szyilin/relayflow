package com.relayflow.module.agent.controller.app.def.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AgentDefSaveReqVO {

    private Long id;

    @NotBlank
    @Size(max = 128)
    private String name;

    @Size(max = 512)
    private String description;

    @Size(max = 20000)
    private String systemPrompt;

    @NotBlank
    @Size(max = 64)
    private String modelRef;

    /** active | disabled; default active on create */
    @Size(max = 16)
    private String status;

    /** Bound skill codes from catalog; empty/null means no tools. */
    private List<String> skillIds;
}
