package com.relayflow.module.agent.controller.admin.llm.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LlmConnectionSaveReqVO {

    private Long id;

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,63}$", message = "编码格式无效")
    private String code;

    @NotBlank
    @Size(max = 128)
    private String name;

    /** Defaults to openai_compatible when blank. */
    @Size(max = 32)
    private String providerType;

    @NotBlank
    @Size(max = 512)
    private String baseUrl;

    /** Blank on update keeps previous key. */
    @Size(max = 1024)
    private String apiKey;

    @NotBlank
    @Size(max = 128)
    private String modelName;

    @NotNull
    private Boolean enabled;
}
