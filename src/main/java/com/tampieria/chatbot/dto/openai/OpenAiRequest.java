package com.tampieria.chatbot.dto.openai;

import java.util.List;

public record OpenAiRequest(String model, List<OpenAiMessage> messages) {
}
