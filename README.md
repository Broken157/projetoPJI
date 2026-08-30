# Palco — projeto integrado

Projeto unificado para apresentação parcial do PJI:

- `frontend`: React/Create React App e telas HTML legadas, desenvolvidos separadamente;
- `backend`: API Spring Boot 4 com autenticação JWT;
- `database`: schema e migrações PostgreSQL.

## Pré-requisitos

- Java 21;
- Node.js 22.x com npm;
- Docker Desktop.

## 1. Banco de dados

Na raiz do projeto:

```powershell
docker compose up -d database
```

O container cria o banco `portifoliodb`, executa `database/sos_artistas.sql` e depois `database/migration_rf03.sql`.

## 2. Desenvolvimento

O desenvolvimento continua separado. Em um terminal, inicie o backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

A API fica disponível em `http://localhost:8080/api`.

Em outro terminal, inicie o React:

```powershell
cd frontend
npm start
```

O frontend de desenvolvimento fica em `http://localhost:3000`.

Variáveis aceitas pelo backend:

- `DB_URL` — padrão `jdbc:postgresql://localhost:5434/portifoliodb`;
- `DB_USER` — padrão `postgres`;
- `DB_PASSWORD` — padrão `1234`;
- `FRONTEND_ORIGINS` — padrão `http://localhost:3000,http://127.0.0.1:3000`;
- `JWT_SECRET` — segredo usado para assinar tokens.

## 3. Build de produção unificado

Na raiz do projeto, o orquestrador executa `npm ci`, compila o frontend e
empacota o JAR Spring:

```powershell
.\build-production.ps1
```

O Maven incorpora `frontend/build` em `BOOT-INF/classes/static` durante o
processamento de recursos. O diretório `frontend/build` continua ignorado pelo
Git e nenhum arquivo gerado é copiado para `backend/src/main/resources`.

Comandos equivalentes para Windows:

```powershell
cd frontend
npm ci
npm run build
cd ..\backend
.\mvnw.cmd clean package
```

Em Linux/macOS ou no ambiente de deploy, use os mesmos passos com
`cd frontend && npm ci && npm run build` e depois
`cd ../backend && ./mvnw clean package`.

O JAR gerado em `backend/target` serve, pela mesma origem:

- React em `/` e nas rotas autorizadas `/vagas/**`;
- API em `/api/**`;
- WebSocket/STOMP em `/ws`;
- páginas legadas `.html` e seus assets literais.

Não existe fallback SPA global: caminhos desconhecidos e assets ausentes
continuam retornando erro real.

## Fluxo de apresentação

1. Cadastre um contratante em `cadastro-contratante.html`.
2. Faça login.
3. Complete biografia e localização em `perfil.html`.
4. Publique uma vaga pelo dashboard.
5. Consulte, edite e cancele a vaga em `minhas-vagas.html`.

O cancelamento é lógico: a vaga passa a `CANCELADA`, as candidaturas são preservadas e o evento é registrado em `log_vagas_canceladas`.

## Testes

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm test -- --watchAll=false
npm run build
```

Os testes de integração usam Testcontainers e exigem Docker em execução.
