package com.tampieria.chatbot.entity.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RoleEnumTest {

    @Test
    void deveConverterValorDaApiParaEnum() {
        assertThat(RoleEnum.fromValor("user")).isEqualTo(RoleEnum.USER);
        assertThat(RoleEnum.USER.getValor()).isEqualTo("user");
    }
}
