# WesselTrack

Aplicação de controle financeiro com:

- `backend/`: API Spring Boot
- `frontend/`: interface Angular

## Desenvolvimento local

### Backend

Requisitos:

- Java 21

Execução:

```bash
cd backend
./mvnw spring-boot:run
```

Variáveis opcionais:

- `APP_JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS`
- `DATABASE_URL`
- `SPRING_DATASOURCE_URL`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`

Sem variáveis externas, o backend usa SQLite local em `backend/finance.db`.

O backend aceita automaticamente:

- `jdbc:sqlite:...`
- `jdbc:postgresql://...`
- `postgresql://...` (formato comum do Render)

### Frontend

Requisitos:

- Node 20+

Execução:

```bash
cd frontend
npm ci
npm start
```

Por padrão, o frontend lê `frontend/public/app-config.json` e usa:

```json
{
  "apiBaseUrl": "http://localhost:8080/api"
}
```

## Deploy manual no Render

### 1. Banco Postgres

No Render:

- `New +` > `Postgres`
- crie um banco para a aplicação
- copie a `External Database URL` ou `Internal Database URL`

### 2. Backend

No Render:

- `New +` > `Web Service`
- selecione o repositório
- configure:

- `Root Directory`: `backend`
- `Runtime`: `Docker`
- `Dockerfile Path`: `./Dockerfile`
- `Health Check Path`: `/actuator/health`

Variáveis de ambiente:

- `APP_JWT_SECRET`: gere um valor forte
- `APP_CORS_ALLOWED_ORIGINS`: URL pública do frontend
- `DATABASE_URL`: URL do Postgres fornecida pelo Render
- `SPRING_JPA_HIBERNATE_DDL_AUTO`: `update`

Exemplo:

```text
APP_CORS_ALLOWED_ORIGINS=https://app.seudominio.com
DATABASE_URL=postgresql://usuario:senha@host:5432/wesseltrack
```

### 3. Frontend

No Render:

- `New +` > `Static Site`
- selecione o mesmo repositório
- configure:

- `Root Directory`: `frontend`
- `Build Command`: `sh scripts/render-build.sh`
- `Publish Directory`: `dist/frontend/browser`

Variável de ambiente:

- `API_BASE_URL`: URL pública do backend com `/api` no final

Exemplo:

```text
API_BASE_URL=https://api.seudominio.com/api
```

### 4. Domínio próprio

Sugestão:

- frontend: `app.seudominio.com`
- backend: `api.seudominio.com`

Valores correspondentes:

```text
APP_CORS_ALLOWED_ORIGINS=https://app.seudominio.com
API_BASE_URL=https://api.seudominio.com/api
```

Depois do deploy:

1. adicione o domínio customizado em cada serviço no Render
2. crie os registros DNS no seu provedor
3. valide o domínio no painel do Render

## Observações

- O frontend lê a URL da API em runtime via `app-config.json`, então o deploy não fica preso em `localhost`.
- O backend foi preparado para aceitar a URL do Postgres do Render sem conversão manual.
- O projeto ainda usa `spring.jpa.hibernate.ddl-auto=update` por praticidade. Para produção mais segura, o próximo passo recomendado é adotar migrações versionadas.
