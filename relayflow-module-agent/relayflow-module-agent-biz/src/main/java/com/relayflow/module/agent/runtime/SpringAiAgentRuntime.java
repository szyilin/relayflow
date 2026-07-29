package com.relayflow.module.agent.runtime;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.agent.enums.ErrorCodeConstants;
import com.relayflow.module.agent.llm.LlmConnectionResolver;
import com.relayflow.module.agent.llm.ResolvedLlmConnection;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring AI OpenAI-compatible runtime. Builds ChatClient per turn from tenant connection (no yml API key).
 */
@Service
@RequiredArgsConstructor
public class SpringAiAgentRuntime implements AgentRuntime {

    private final LlmConnectionResolver llmConnectionResolver;

    @Override
    public AgentRunResult run(AgentRunCommand command) {
        if (command == null || !StringUtils.hasText(command.getUserMessage())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_MESSAGE_EMPTY);
        }
        try {
            ChatClient.ChatClientRequestSpec spec = buildPrompt(command);
            String content = spec.call().content();
            if (!StringUtils.hasText(content)) {
                content = "";
            }
            return AgentRunResult.builder().assistantText(content).build();
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeConstants.AGENT_RUNTIME_FAILED.getCode(),
                    "智能体运行失败：" + ex.getMessage());
        }
    }

    @Override
    public String stream(AgentRunCommand command, AgentStreamListener listener, AtomicBoolean cancelFlag) {
        if (command == null || !StringUtils.hasText(command.getUserMessage())) {
            throw new ServiceException(ErrorCodeConstants.AGENT_MESSAGE_EMPTY);
        }
        AgentStreamListener safeListener = listener == null ? e -> {
        } : listener;
        AtomicBoolean cancel = cancelFlag == null ? new AtomicBoolean(false) : cancelFlag;
        StringBuilder fullText = new StringBuilder();
        try {
            ChatClient.ChatClientRequestSpec spec = buildPrompt(command);
            Flux<String> contentFlux = spec.stream().content();
            contentFlux
                    .takeWhile(chunk -> !cancel.get())
                    .doOnNext(chunk -> {
                        if (chunk == null || chunk.isEmpty() || cancel.get()) {
                            return;
                        }
                        fullText.append(chunk);
                        safeListener.onEvent(AgentStreamEvent.textDelta(chunk));
                    })
                    .blockLast();
            if (!cancel.get()) {
                safeListener.onEvent(AgentStreamEvent.done());
            }
            return fullText.toString();
        } catch (ServiceException ex) {
            safeListener.onEvent(AgentStreamEvent.error(ex.getMessage(), ex.getCode()));
            throw ex;
        } catch (Exception ex) {
            String msg = "智能体运行失败：" + ex.getMessage();
            safeListener.onEvent(AgentStreamEvent.error(msg, ErrorCodeConstants.AGENT_RUNTIME_FAILED.getCode()));
            throw new ServiceException(ErrorCodeConstants.AGENT_RUNTIME_FAILED.getCode(), msg);
        }
    }

    private ChatClient.ChatClientRequestSpec buildPrompt(AgentRunCommand command) {
        ResolvedLlmConnection connection = llmConnectionResolver.requireEnabled(command.getModelRef());
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(toOpenAiBaseUrl(connection.getBaseUrl()))
                .apiKey(connection.getApiKey())
                .build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(connection.getModelName())
                        .build())
                .build();
        ChatClient chatClient = ChatClient.create(chatModel);

        List<Message> history = command.getHistory() == null ? List.of() : command.getHistory();
        List<ToolCallback> tools = command.getTools() == null ? List.of() : command.getTools();

        var spec = chatClient.prompt()
                .system(command.getSystemPrompt() == null ? "" : command.getSystemPrompt())
                .messages(new ArrayList<>(history))
                .user(command.getUserMessage().trim());
        if (!tools.isEmpty()) {
            spec = spec.toolCallbacks(tools);
        }
        return spec;
    }

    /**
     * Spring AI {@link OpenAiApi} expects host root (e.g. https://api.openai.com);
     * stored connections often include {@code /v1} for models probing.
     */
    static String toOpenAiBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed;
    }
}
