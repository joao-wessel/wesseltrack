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
- `postgresql://...`

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

## Deploy em VPS (KingHost)

### Arquitetura recomendada

- `Nginx` para servir o frontend e fazer proxy da API
- `Backend Spring Boot` rodando em `127.0.0.1:8080`
- `PostgreSQL` para persistência
- domínio público no frontend e subdomínio para API

Sugestão de domínios:

- frontend: `wesseltrack.wessel.dev.br`
- backend: `api.wesseltrack.wessel.dev.br`

### 1. Preparar a VPS

Instale na VPS:

- `git`
- `nginx`
- `postgresql`
- `openjdk-21-jre` ou `openjdk-21-jdk`
- `nodejs` + `npm` (apenas se for buildar o frontend na VPS)

Em Ubuntu/Debian, por exemplo:

```bash
sudo apt update
sudo apt install -y git nginx postgresql postgresql-contrib openjdk-21-jre unzip
```

### 2. Banco PostgreSQL

Crie o banco e usuário:

```bash
sudo -u postgres psql
```

```sql
create database wesseltrack;
create user wesseltrack_user with encrypted password 'SENHA_FORTE_AQUI';
grant all privileges on database wesseltrack to wesseltrack_user;
\q
```

### 3. Clonar o projeto

```bash
cd /opt
sudo git clone SEU_REPOSITORIO_GIT wesseltrack
sudo chown -R $USER:$USER /opt/wesseltrack
cd /opt/wesseltrack
```

### 4. Subir o backend

Empacote o jar:

```bash
cd /opt/wesseltrack/backend
./mvnw clean package -DskipTests
```

Crie um arquivo de ambiente, por exemplo `/opt/wesseltrack/backend/.env`:

```bash
APP_JWT_SECRET=UMA_CHAVE_LONGA_E_FORTE
APP_CORS_ALLOWED_ORIGINS=https://wesseltrack.wessel.dev.br
DATABASE_URL=postgresql://wesseltrack_user:SENHA_FORTE_AQUI@127.0.0.1:5432/wesseltrack
SPRING_JPA_HIBERNATE_DDL_AUTO=update
PORT=8080
```

Crie o serviço `systemd` em `/etc/systemd/system/wesseltrack-api.service`:

```ini
[Unit]
Description=WesselTrack API
After=network.target postgresql.service

[Service]
User=www-data
WorkingDirectory=/opt/wesseltrack/backend
EnvironmentFile=/opt/wesseltrack/backend/.env
ExecStart=/usr/bin/java -jar /opt/wesseltrack/backend/target/backend-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Ative o serviço:

```bash
sudo systemctl daemon-reload
sudo systemctl enable wesseltrack-api
sudo systemctl start wesseltrack-api
sudo systemctl status wesseltrack-api
```

Teste localmente na VPS:

```bash
curl http://127.0.0.1:8080/actuator/health
```

### 5. Buildar o frontend

Se for buildar na VPS:

```bash
cd /opt/wesseltrack/frontend
API_BASE_URL=https://api.wesseltrack.wessel.dev.br/api sh scripts/build-with-config.sh
```

Isso vai gerar os arquivos em:

```text
/opt/wesseltrack/frontend/dist/frontend/browser
```

### 6. Configurar Nginx

Crie um arquivo `/etc/nginx/sites-available/wesseltrack`:

```nginx
server {
    listen 80;
    server_name wesseltrack.wessel.dev.br;

    root /opt/wesseltrack/frontend/dist/frontend/browser;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}

server {
    listen 80;
    server_name api.wesseltrack.wessel.dev.br;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Ative a configuração:

```bash
sudo ln -s /etc/nginx/sites-available/wesseltrack /etc/nginx/sites-enabled/wesseltrack
sudo nginx -t
sudo systemctl reload nginx
```

### 7. DNS no Registro.br

Crie:

- `wesseltrack` apontando para o IP da VPS
- `api.wesseltrack` apontando para o IP da VPS

Como a VPS usa IP próprio, no Registro.br o mais comum será usar registros `A`:

- `wesseltrack` -> `IP_DA_VPS`
- `api.wesseltrack` -> `IP_DA_VPS`

### 8. HTTPS

Instale Certbot:

```bash
sudo apt install -y certbot python3-certbot-nginx
```

Gere os certificados:

```bash
sudo certbot --nginx -d wesseltrack.wessel.dev.br -d api.wesseltrack.wessel.dev.br
```

### 9. Criar o primeiro usuário administrador

Depois que a API estiver pública:

```bash
curl -X POST https://api.wesseltrack.wessel.dev.br/api/auth/bootstrap-admin \
  -H "Content-Type: application/json" \
  -d '{"name":"Administrador","username":"admin","password":"SuaSenhaForte123"}'
```

## Observações

- O frontend lê a URL da API em runtime via `app-config.json`, então não fica preso em `localhost`.
- O backend aceita a URL do banco em formato PostgreSQL padrão.
- O projeto ainda usa `spring.jpa.hibernate.ddl-auto=update` por praticidade. Para produção mais segura, o próximo passo recomendado é adotar migrações versionadas.
