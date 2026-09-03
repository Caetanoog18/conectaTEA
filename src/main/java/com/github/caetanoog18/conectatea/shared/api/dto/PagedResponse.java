package com.github.caetanoog18.conectatea.shared.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        @Schema(description = "Registros presentes nesta página")
        List<T> content,

        @Schema(description = "Índice da página, começando em zero", example = "0")
        int page,

        @Schema(description = "Tamanho solicitado para a página", example = "20")
        int size,

        @Schema(description = "Total de registros da consulta")
        long totalElements,

        @Schema(description = "Total de páginas da consulta")
        int totalPages
) {
    public static <T> PagedResponse<T> from(Page<T> result) {
        return new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }
}