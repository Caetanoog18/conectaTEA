# ConectaTEA API

Backend em desenvolvimento para acompanhamento educacional e
multiprofissional de estudantes em uma única instituição escolar.

O sistema organiza estudantes, responsáveis, consentimentos, profissionais,
observações, linha do tempo, auditoria e relatórios.

> Este projeto ainda está em desenvolvimento e não deve ser usado
> em produção.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL 17
- Flyway
- Docker Compose
- Testcontainers
- JUnit e MockMvc
- Springdoc OpenAPI
- OpenPDF

## Módulos

| Módulo | Responsabilidade |
|---|---|
| `identity` | Usuários, autenticação e autorização |
| `institution` | Dados da instituição |
| `student` | Cadastro dos estudantes |
| `guardian` | Responsáveis e vínculos com estudantes |
| `consent` | Consentimentos, finalidades e revogações |
| `careteam` | Vínculos entre estudantes e profissionais |
| `observation` | Registros educacionais e multiprofissionais |
| `timeline` | Linha do tempo dos estudantes |
| `audit` | Registro e consulta de operações sensíveis |
| `report` | Relatórios em JSON e PDF |

## Requisitos

- JDK 21
- Docker
- Docker Compose

O Maven Wrapper já está incluído no projeto.

## Banco de dados

Inicie o PostgreSQL:

```bash
docker compose up -d
```

Confira:

```bash
docker compose ps
```

A configuração local padrão utiliza:

```text
Database: conectatea
Username: conectatea
Port: 5432
```

As tabelas são gerenciadas exclusivamente pelas migrations do Flyway.

## Variáveis de ambiente

Gerar um JWT SECRET em Base64:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```

Para criar o primeiro administrador em um banco vazio:

```bash
export INITIAL_ADMIN_NAME="System Administrator"
export INITIAL_ADMIN_EMAIL="admin@example.com"

read -rsp "Initial administrator password: " INITIAL_ADMIN_PASSWORD
printf '\n'
export INITIAL_ADMIN_PASSWORD
```

A senha inicial deve ter pelo menos 12 caracteres.

Não salve senhas ou o segredo JWT no repositório !!!.

## Executar a aplicação

```bash
./mvnw spring-boot:run
```

Verifique:

```bash
curl http://localhost:8080/actuator/health
```

Resultado esperado:

```json
{"status":"UP"}
```

## Executar os testes

```bash
./mvnw clean test
```

Os testes de integração usam o PostgreSQL através do Testcontainers.

## Documentação OpenAPI

A documentação fica desativada por padrão.

Para habilitá-la localmente, execute o comando abaixo:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
./mvnw spring-boot:run -Dspring-boot.run.profiles=docs
```

Acesse:

```text
http://localhost:8080/swagger-ui.html
```

Especificação JSON:

```text
http://localhost:8080/v3/api-docs
```

No botão **Authorize**, informe somente o `accessToken`, sem escrever
manualmente o prefixo `Bearer`.

Utilize apenas dados fictícios nos testes realizados pelo Swagger.

## Autorização de acesso aos estudantes

O JWT sozinho não da acesso aos dados de um estudante.

O sistema também verifica:

1. status atual da conta no banco;
2. perfil atual do profissional;
3. status do estudante;
4. vínculo profissional vigente;
5. responsável legal ativo;
6. consentimento ativo e dentro da validade;
7. finalidades autorizadas no mesmo termo.

Professores precisam das finalidades:

```text
EDUCATIONAL_SUPPORT
INFORMATION_SHARING_WITH_CARE_TEAM
```

Profissionais multiprofissionais precisam de:

```text
MULTIPROFESSIONAL_MONITORING
INFORMATION_SHARING_WITH_CARE_TEAM
```

Relatórios também exigem:

```text
REPORT_GENERATION
```

## Auditoria

As operações sensíveis abaixo são auditadas:

- criação e consulta de observações;
- consulta da linha do tempo;
- consulta dos eventos de auditoria;
- geração de relatórios JSON;
- exportação de relatórios PDF.

As respostas auditadas incluem o header:

```text
X-Request-ID
```

Ele permite correlacionar a requisição com o evento armazenado no banco.

## Segurança

- autenticação stateless com JWT HS256;
- senhas armazenadas com BCrypt;
- Swagger desativado por padrão;
- documentação local vinculada a `127.0.0.1`;
- autorização por perfil, vínculo e consentimento;
- respostas restritas para evitar revelar estudantes inacessíveis;
- credenciais configuradas por variáveis de ambiente.

Dados pessoais e informações de saúde exigem medidas adicionais conforme dispõe
a Lei Complementar 13709/2018 conhecida como LGPD.