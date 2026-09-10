package com.github.caetanoog18.conectatea.audit.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
                Resultado da operação auditada.

                SUCCESS: operação concluída com sucesso.
                DENIED: operação recusada por autenticação ou autorização.
                FAILURE: operação rejeitada por outro erro.
                """
)
public enum AuditOutcome {
    SUCCESS,
    DENIED,
    FAILURE
}