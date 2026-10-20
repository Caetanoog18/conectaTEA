package com.github.caetanoog18.conectatea.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {
    @Test
    void shouldNormalizeFormattedCpf() {
        assertThat(Cpf.normalize("529.982.247-25")).isEqualTo("52998224725");
    }

    @Test
    void shouldPreserveLeadingZeros() {
        assertThat(Cpf.normalize("01234567890")).isEqualTo("01234567890");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "11111111111",
            "12345678900",
            "52998224724",
            "5299822472",
            "abc52998224725"
    })
    void shouldRejectInvalidCpf(String value) {
        assertThatThrownBy(() -> Cpf.normalize(value)).isInstanceOf(InvalidCpfException.class);
    }
}