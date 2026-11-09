package com.tampieria.chatbot.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoleEnum {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    private final String valor;

    RoleEnum(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static RoleEnum fromValor(String valor) {
        for (RoleEnum role : values()) {
            if (role.valor.equalsIgnoreCase(valor)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Role inválido: " + valor);
    }
}
