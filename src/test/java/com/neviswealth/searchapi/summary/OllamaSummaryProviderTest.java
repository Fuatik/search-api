package com.neviswealth.searchapi.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class OllamaSummaryProviderTest {

    private ChatModel chatModel;
    private OllamaSummaryProvider provider;

    @BeforeEach
    void setUp() {
        chatModel = Mockito.mock(ChatModel.class);
        provider = new OllamaSummaryProvider(chatModel);
    }

    @Test
    void nullContentReturnsEmptyStringAndDoesNotCallModel() {
        assertThat(provider.summarize(null)).isEmpty();
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void blankContentReturnsEmptyStringAndDoesNotCallModel() {
        assertThat(provider.summarize("   ")).isEmpty();
        verify(chatModel, never()).call(any(Prompt.class));
    }

    @Test
    void validContentCallsModelAndReturnsTrimmedText() {
        ChatResponse response = Mockito.mock(ChatResponse.class);
        Generation generation = Mockito.mock(Generation.class);
        AssistantMessage message = Mockito.mock(AssistantMessage.class);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn("  short summary.  ");
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        String content = "A document body to summarize.";
        String summary = provider.summarize(content);

        assertThat(summary).isEqualTo("short summary.");
        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());
        assertThat(promptCaptor.getValue().getContents()).contains(content);
    }

    @Test
    void nullModelTextReturnsEmptyString() {
        ChatResponse response = Mockito.mock(ChatResponse.class);
        Generation generation = Mockito.mock(Generation.class);
        AssistantMessage message = Mockito.mock(AssistantMessage.class);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn(null);
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        assertThat(provider.summarize("Some content")).isEmpty();
    }
}