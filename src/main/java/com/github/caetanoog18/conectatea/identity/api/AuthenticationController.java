package com.github.caetanoog18.conectatea.identity.api;

import com.github.caetanoog18.conectatea.identity.api.dto.AuthenticatedUserResponse;
import com.github.caetanoog18.conectatea.identity.api.dto.LoginRequest;
import com.github.caetanoog18.conectatea.identity.api.dto.TokenResponse;
import com.github.caetanoog18.conectatea.identity.application.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Autenticação",
        description = "Login e consulta dos dados presentes no token"
)
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(
            AuthenticationService authenticationService
    ) {
        this.authenticationService = authenticationService;
    }

    @Operation(
            operationId = "login",
            summary = "Autenticar uma conta",
            description = """
                    Recebe e-mail e senha e devolve um JWT.

                    Não exige um token anterior. Utilize o accessToken
                    retornado no botão Authorize do Swagger.

                    A posse do token não concede acesso automático
                    a todos os estudantes ou funcionalidades.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticação realizada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokenResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "JSON inválido ou credenciais com campos inválidos",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = """
                            Autenticação rejeitada. Credenciais recusadas pelo
                            serviço retornam ProblemDetail. Uma rejeição anterior,
                            no filtro JWT, pode não incluir esse corpo.
                            """,
                    content = @Content(mediaType =
                            "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @Operation(
            operationId = "getCurrentUser",
            summary = "Consultar os dados do token atual",
            description = """
                    Retorna o subject e os perfis presentes no JWT validado.

                    Esta operação não consulta novamente o cadastro do usuário.
                    Portanto, não deve ser usada como prova de que o usuário
                    continua ativo ou mantém as mesmas permissões no banco.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dados presentes no token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation = AuthenticatedUserResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token ausente, inválido ou expirado",
                    content = @Content
            )
    })
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> currentUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(new AuthenticatedUserResponse(jwt.getSubject(), jwt.getClaimAsStringList("roles")));
    }
}