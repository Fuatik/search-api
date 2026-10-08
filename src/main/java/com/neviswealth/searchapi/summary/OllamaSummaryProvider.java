package com.neviswealth.searchapi.summary;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Requires a running Ollama instance with llama3.2:1b pulled. If Ollama is unreachable, the ChatModel call throws and the request fails — there is no fallback.
 */
@Component
@ConditionalOnProperty(name = "app.summary.provider", havingValue = "ollama")
public class OllamaSummaryProvider implements SummaryProvider {

    private final ChatModel chatModel;

    public OllamaSummaryProvider(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String prompt = "Summarize the following document in 2-3 sentences. Return only the summary, no preamble.\n\n"
                + content;
        ChatResponse response = chatModel.call(new Prompt(prompt));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }

        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text.trim();
    }
}