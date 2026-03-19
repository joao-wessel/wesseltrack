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
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_DRIVER_CLASS_NAME`
- `SPRING_JPA_DATABASE_PLATFORM`
- `SPRING_JPA_HIBERNATE_DDL_AUTO`

Sem variáveis externas, o backend usa SQLite local em `backend/finance.db`.

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

## Deploy no Render

O repositório já inclui `render.yaml` com:

- `wesseltrack-api`: Web Service Docker (Starter)
- `wesseltrack-web`: Static Site Angular
- `wesseltrack-db`: Postgres gerenciado (Basic 256 MB)

### Como publicar

1. Suba este repositório para o GitHub.
2. No Render, escolha `New +` > `Blueprint`.
3. Selecione o repositório.
4. Confirme a criação dos 3 recursos definidos no `render.yaml`.`r`n5. Durante a criação do Blueprint, informe manualmente:`r`n   - `API_BASE_URL`: URL pública da API com `/api` no final. Ex.: `https://wesseltrack-api.onrender.com/api``r`n   - `APP_CORS_ALLOWED_ORIGINS`: URL pública do frontend. Ex.: `https://wesseltrack-web.onrender.com`
6. Aguarde o primeiro deploy.`r`n7. No primeiro acesso, crie o usuário administrador se o banco estiver vazio.

### O que o blueprint faz

- backend:
  - builda em `backend/`
  - expõe healthcheck em `/actuator/health`
  - recebe `APP_JWT_SECRET` automaticamente
  - usa Postgres do Render
- frontend:
  - builda em `frontend/`
  - gera `public/app-config.json` durante o build com a URL da API
  - publica a SPA com rewrite para `index.html`

### Observações

- Em produção, use serviço pago no Render. A documentação atual do Render informa que o runtime nativo não inclui Java, então o backend foi preparado com Docker.
- O projeto ainda usa `spring.jpa.hibernate.ddl-auto=update` por praticidade. Para evolução segura em produção, o próximo passo recomendado é adotar migrações versionadas.`r`n- O frontend lê a URL da API em `app-config.json`, então o deploy não fica preso em `localhost`.


