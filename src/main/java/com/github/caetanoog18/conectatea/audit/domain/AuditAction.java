package com.github.caetanoog18.conectatea.audit.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
                Ação sensível registrada na auditoria.

                OBSERVATION_CREATE: criação de observação.
                OBSERVATION_LIST: listagem de observações.
                OBSERVATION_READ: consulta de observação.
                TIMELINE_READ: consulta da linha do tempo.
                AUDIT_EVENTS_LIST: consulta da própria auditoria.
                REPORT_GENERATE: geração de relatório JSON.
                REPORT_PDF_EXPORT: geração de relatório PDF.
                """
)
public enum AuditAction {
    OBSERVATION_CREATE,
    OBSERVATION_LIST,
    OBSERVATION_READ,
    TIMELINE_READ,
    AUDIT_EVENTS_LIST,
    REPORT_GENERATE,
    REPORT_PDF_EXPORT
}