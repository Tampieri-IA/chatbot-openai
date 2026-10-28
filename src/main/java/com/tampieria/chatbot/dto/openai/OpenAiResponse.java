package com.tampieria.chatbot.dto.openai;

import java.util.List;

public record OpenAiResponse(List<OpenAiChoice> choices) {
}
