# Execução integrada do ConectaTEA

O ambiente integrado executa PostgreSQL 17, a API Java e o frontend
Angular. O navegador acessa a aplicação pelo Nginx.

## Requisitos

- Docker com Docker Compose v2 e suporte a `up --wait`.
- OpenSSL para gerar os segredos locais.
- Para executar as suítes fora dos containers: JDK 21 e Node.js 24.

## Configuração inicial

Na raiz do repositório, crie um arquivo `.env.final` com permissão 600.
Esse arquivo é local e não deve ser versionado.

| Variável | Finalidade |
|---|---|
| `FINAL_DB_PASSWORD` | Senha do PostgreSQL do ambiente integrado |
| `FINAL_JWT_SECRET` | Segredo JWT em Base64; gerar com `openssl rand -base64 48` |
| `FINAL_ADMIN_NAME` | Nome do primeiro administrador |
| `FINAL_ADMIN_EMAIL` | E-mail usado no primeiro login |
| `FINAL_ADMIN_PASSWORD` | Senha inicial forte, com pelo menos 12 caracteres |
| `FINAL_ADMIN_CPF` | CPF válido do administrador; usar dado de teste na demonstração |
| `FINAL_WEB_PORT` | Porta local do frontend; padrão: 8088 |

Para o banco e para a senha inicial, use valores distintos.
A conta inicial é criada durante a inicialização de um banco vazio.

## Iniciar

Execute na raiz do projeto:

```bash
docker compose --env-file .env.final -f compose.full.yaml config --quiet
docker compose --env-file .env.final -f compose.full.yaml up -d --build --wait
```

Abra `http://localhost:8088`. Se alterou `FINAL_WEB_PORT`, use a porta
configurada. Entre com o e-mail e a senha do administrador inicial.

## Verificar

```bash
docker compose --env-file .env.final -f compose.full.yaml ps
curl -fsS http://localhost:8088/actuator/health
```

## Executar os testes

```bash
./mvnw clean test
(cd frontend && npm ci && npm test -- --watch=false && npm run build)
```

## Parar preservando os dados

```bash
docker compose --env-file .env.final -f compose.full.yaml down
```

O banco integrado usa um volume próprio. Ele é independente do banco
do `compose.yaml` usado no desenvolvimento. Não use `down -v` se quiser
preservar os dados.

## Escopo

Este ambiente foi preparado para execução local e demonstração.
Somente o frontend é publicado, em uma porta da interface local.