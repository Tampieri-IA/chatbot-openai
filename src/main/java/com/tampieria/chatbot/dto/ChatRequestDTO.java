package com.tampieria.chatbot.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDTO(
        @NotBlank(message = "sessaoId é obrigatório")
        String sessaoId,

        @NotBlank(message = "mensagem não pode estar vazia")
        String mensagemUsuario
) { }
