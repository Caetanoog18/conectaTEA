package com.github.caetanoog18.conectatea.identity.api;

import com.github.caetanoog18.conectatea.identity.api.dto.CreateUserRequest;
import com.github.caetanoog18.conectatea.shared.api.dto.PagedResponse;
import com.github.caetanoog18.conectatea.identity.api.dto.UpdateUserStatusRequest;
import com.github.caetanoog18.conectatea.identity.api.dto.UserResponse;
import com.github.caetanoog18.conectatea.identity.application.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
@Tag(
        name = "Usuários",
        description = """
                Administração das contas da aplicação.
                Exige a autoridade ADMINISTRATOR no JWT.
                Não existe cadastro público de usuários.
                """
)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Token ausente, inválido ou expirado"

        ),
        @ApiResponse(
                responseCode = "403",
                description = "Token sem a autoridade ADMINISTRATOR"

        )
})
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            operationId = "createUser",
            summary = "Cadastrar usuário",
            description = """
                Cria uma conta ativa com o perfil informado.
                O e-mail deve ser único, desconsiderando maiúsculas e minúsculas.
                A senha é recebida em texto e armazenada como hash.
                A resposta não contém a senha nem o hash.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Usuário criado",
                            useReturnTypeSchema = true,
                            headers = @Header(
                                    name = "Location",
                                    description = "Endereço do usuário criado",
                                    schema = @Schema(type = "string", format = "uri")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dados de cadastro inválidos"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "E-mail já utilizado"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            operationId = "listUsers",
            summary = "Listar usuários",
            description = """
                Lista contas ativas e inativas, ordenadas por fullName crescente.
                A primeira página é zero.
                Não há filtro de status nem parâmetro de ordenação neste endpoint.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Página de usuários",
                            useReturnTypeSchema = true
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Parâmetros de paginação inválidos"
                    )
            }
    )
    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> findAll(
            @Parameter(description = "Página solicitada, começando em zero")
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,

            @Parameter(description = "Quantidade por página: de 1 a 100")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Size must be at least 1")
            @Max(value = 100, message = "Size must be at most 100")
            int size
    ) {
        return ResponseEntity.ok(userService.findAll(page, size));
    }

    @Operation(
            operationId = "getUser",
            summary = "Consultar usuário pelo identificador",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuário encontrado",
                            useReturnTypeSchema = true
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador com formato inválido"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado"
                    )
            }
    )
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.findById(userId));
    }

    @Operation(
            operationId = "updateUserStatus",
            summary = "Ativar ou desativar usuário",
            description = """
                Recebe active=true para ativar ou active=false para desativar.
                O campo é obrigatório.
                O administrador não pode desativar a própria conta.
                O registro não é excluído.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Status atualizado",
                            useReturnTypeSchema = true

                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador ou corpo inválido"

                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado"

                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Tentativa de desativar a própria conta"
                    )
            }
    )
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(userService.updateStatus(userId, request, jwt.getSubject()));
    }
}