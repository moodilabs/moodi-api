package com.moodi.spot.infrastructure.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String content) {}

    public String extractContent() {
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("LLM 응답에 choices가 없습니다");
        }
        Message message = choices.getFirst().message();
        if (message == null || message.content() == null) {
            throw new IllegalStateException("LLM 응답에 content가 없습니다");
        }
        return message.content().strip();
    }
}
