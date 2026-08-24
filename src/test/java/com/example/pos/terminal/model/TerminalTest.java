package com.example.pos.terminal.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

class TerminalTest {

    @Test
    void generatedApiSecretFitsTheMappedColumn() throws NoSuchFieldException {
        Column column = Terminal.class.getDeclaredField("apiSecret").getAnnotation(Column.class);
        String secret = Terminal.generateApiSecret();

        assertThat(secret).hasSize(22);
        assertThat(column.length()).isGreaterThanOrEqualTo(secret.length());
    }
}
