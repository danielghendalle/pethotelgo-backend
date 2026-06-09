# Deploy — Oracle Cloud Free Tier

Este guia cobre todo o processo de publicação do **PetHotelGO** (backend Spring Boot + frontend React) em uma instância Oracle Cloud Always Free usando Docker Compose.

---

## Visão geral da arquitetura

```
Internet
  │
  ▼  porta 80 (HTTP) ou 443 (HTTPS)
┌─────────────────────────────────┐
│           Nginx (proxy)         │
│  /api/*  →  Spring Boot :8080   │
│  /*      →  React (Nginx) :80   │
└─────────────────────────────────┘
        │                │
        ▼                ▼
   Spring Boot       Frontend
  (Kotlin API)      (React/Vite
   porta 8080       Nginx :80)
        │
        ▼
   PostgreSQL 16
   porta 5432 (interno)
```

---

## Pré-requisitos

- Conta Oracle Cloud com Always Free Tier ativado
- Par de chaves SSH (pública/privada)
- Credenciais do Firebase (`firebase-service-account.json`)
- Repositórios no GitHub (backend e frontend)

---

## Parte 1 — Criar a instância Oracle Cloud

### 1.1 Criar a VM

1. Acesse [cloud.oracle.com](https://cloud.oracle.com) e faça login
2. Menu → **Compute** → **Instances** → **Create Instance**
3. Configure:
   - **Name:** `pethotelgo`
   - **Image:** Ubuntu 22.04 (clique em *Change image*)
   - **Shape:** `VM.Standard.A1.Flex` — Ampere ARM (**Always Free**)
     - OCPUs: `2` (pode usar até 4 no total da conta)
     - Memory: `12 GB` (pode usar até 24 GB no total)
   - **SSH keys:** cole sua chave pública (`~/.ssh/id_rsa.pub`)
4. Clique em **Create**
5. Aguarde o status ficar **Running** e anote o **IP público**

### 1.2 Abrir as portas no firewall da OCI

Sem isso, nada funciona — a OCI bloqueia tudo por padrão.

1. Na página da instância, clique na **VCN** (Virtual Cloud Network)
2. **Security Lists** → clique na security list padrão
3. **Add Ingress Rules** — adicione as seguintes regras:

| Source CIDR | Protocol | Port | Descrição         |
|-------------|----------|------|-------------------|
| `0.0.0.0/0` | TCP      | 80   | HTTP              |
| `0.0.0.0/0` | TCP      | 443  | HTTPS             |

> A porta 22 (SSH) já vem aberta por padrão.

---

## Parte 2 — Configurar o servidor

### 2.1 Conectar via SSH

```bash
ssh -i ~/.ssh/id_rsa ubuntu@<IP_PUBLICO>
```

### 2.2 Executar o script de setup

O script instala Docker, abre as portas no iptables do Ubuntu e clona os repositórios.

```bash
# Baixe o script diretamente do repositório ou copie manualmente
curl -fsSL https://raw.githubusercontent.com/<seu-usuario>/pethotelgo/main/scripts/setup-oracle.sh -o setup.sh
bash setup.sh
```

O script vai pedir as URLs dos dois repositórios:
- Backend: `https://github.com/<seu-usuario>/pethotelgo.git`
- Frontend: `https://github.com/<seu-usuario>/pethotelgo-frontend.git`

Ao final, os repositórios estarão em:
```
~/pethotelgo/            ← backend (compose roda daqui)
~/pethotelgo-frontend/   ← frontend (referenciado como ../pethotelgo-frontend)
```

> **Importante:** após o script, faça logout e login novamente para o Docker funcionar sem `sudo`:
> ```bash
> exit
> ssh -i ~/.ssh/id_rsa ubuntu@<IP_PUBLICO>
> ```

### 2.3 Verificar portas do iptables (Ubuntu Oracle Cloud)

O Ubuntu na OCI tem regras de iptables que bloqueiam tráfego mesmo com o Security List aberto. O script já adiciona as regras, mas confirme:

```bash
sudo iptables -L INPUT --line-numbers | grep -E "80|443"
```

Se não aparecer, adicione manualmente:

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80  -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

---

## Parte 3 — Configurar variáveis de ambiente

```bash
cd ~/pethotelgo
cp .env.example .env
nano .env
```

Preencha cada campo:

```env
# Banco de dados — escolha uma senha forte
POSTGRES_USER=postgres
POSTGRES_PASSWORD=SUA_SENHA_FORTE_AQUI

# JWT — gere com o comando abaixo
JWT_SECRET=
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# CORS — URL pública do frontend (use o IP por enquanto, depois o domínio)
CORS_ALLOWED_ORIGINS=http://<IP_PUBLICO>

# Firebase — gere o base64 do arquivo JSON
FIREBASE_CREDENTIALS_BASE64=
```

### Gerar JWT_SECRET

```bash
openssl rand -base64 64
```

Cole o resultado no `.env`.

### Gerar FIREBASE_CREDENTIALS_BASE64

No **seu Mac/PC local**, converta o arquivo JSON:

```bash
base64 -w 0 firebase-service-account.json
```

No **macOS** use:
```bash
base64 -i firebase-service-account.json | tr -d '\n'
```

Copie a saída (uma linha longa) e cole no `.env`.

---

## Parte 4 — Fazer o deploy

```bash
cd ~/pethotelgo
bash scripts/deploy.sh
```

O script vai:
1. Fazer `git pull` para trazer o código mais recente
2. Construir as imagens Docker (backend Kotlin + frontend React)
3. Subir todos os containers
4. Aguardar o backend ficar saudável (`/api/actuator/health`)
5. Limpar imagens antigas

> **Primeira vez:** o build do backend leva **5–10 minutos** (Maven baixa dependências).

### Verificar se está rodando

```bash
# Status de todos os containers
docker compose -f docker-compose.prod.yml ps

# Logs do backend em tempo real
docker compose -f docker-compose.prod.yml logs -f api

# Logs do nginx
docker compose -f docker-compose.prod.yml logs -f nginx
```

### Testar no navegador

- **Frontend:** `http://<IP_PUBLICO>/`
- **Backend health:** `http://<IP_PUBLICO>/api/actuator/health`

---

## Parte 5 — SSL com HTTPS (opcional, requer domínio)

Se você tiver um domínio (ex: `meuapp.com`), configure HTTPS gratuito com Let's Encrypt.

### 5.1 Apontar DNS

No painel do seu provedor de domínio, crie um registro A:

| Tipo | Nome | Valor           |
|------|------|-----------------|
| A    | `@`  | `<IP_PUBLICO>`  |
| A    | `www`| `<IP_PUBLICO>`  |

Aguarde a propagação (5–30 minutos). Confirme com:

```bash
nslookup meuapp.com
```

### 5.2 Emitir o certificado

```bash
cd ~/pethotelgo
bash scripts/deploy.sh --ssl meuapp.com
```

### 5.3 Ativar o Nginx HTTPS

```bash
cp nginx/conf.d/ssl.conf.example nginx/conf.d/ssl.conf

# Substitua SEU_DOMINIO.com pelo seu domínio
sed -i 's/SEU_DOMINIO.com/meuapp.com/g' nginx/conf.d/ssl.conf

# Remova o config HTTP (agora o SSL faz o redirect 80→443)
rm nginx/conf.d/default.conf

# Reinicie o Nginx
docker compose -f docker-compose.prod.yml restart nginx
```

### 5.4 Atualizar o CORS e rebuild do frontend

Edite o `.env`:

```env
CORS_ALLOWED_ORIGINS=https://meuapp.com,https://www.meuapp.com
```

O `VITE_API_URL` já é `/api` (relativo), então não precisa mudar. Rebuilde:

```bash
docker compose -f docker-compose.prod.yml up -d --build frontend
docker compose -f docker-compose.prod.yml restart nginx
```

O Certbot renova o certificado automaticamente a cada 12 horas.

---

## Parte 6 — Atualizar após mudanças no código

Sempre que fizer push de novas features:

```bash
cd ~/pethotelgo
git pull
cd ~/pethotelgo-frontend
git pull
cd ~/pethotelgo
bash scripts/deploy.sh
```

---

## Referência rápida de comandos

```bash
# Ver status
docker compose -f docker-compose.prod.yml ps

# Logs em tempo real
docker compose -f docker-compose.prod.yml logs -f

# Reiniciar um serviço específico
docker compose -f docker-compose.prod.yml restart api
docker compose -f docker-compose.prod.yml restart nginx
docker compose -f docker-compose.prod.yml restart frontend

# Parar tudo (dados do banco preservados no volume)
docker compose -f docker-compose.prod.yml down

# Parar e apagar o banco de dados (CUIDADO — irreversível)
docker compose -f docker-compose.prod.yml down -v

# Acessar o banco diretamente
docker exec -it pethotelgo-db psql -U postgres -d pethotel

# Ver uso de recursos
docker stats
```

---

## Solução de problemas

### Container `api` reiniciando em loop

```bash
docker compose -f docker-compose.prod.yml logs api --tail=50
```

Causas comuns:
- `JWT_SECRET` vazio no `.env`
- `FIREBASE_CREDENTIALS_BASE64` inválido (base64 com quebra de linha)
- Banco ainda não pronto (aguarde o healthcheck do `db`)

### Nginx retorna 502 Bad Gateway

O backend ainda está subindo. Aguarde ~2 minutos e tente novamente. Verifique:

```bash
docker compose -f docker-compose.prod.yml ps
# api deve estar "healthy", não "starting"
```

### Porta 80 recusada fora do servidor

Verifique as duas camadas de firewall:
1. **OCI Security List** — porta 80 adicionada nas Ingress Rules?
2. **iptables Ubuntu** — `sudo iptables -L INPUT | grep 80`

### Build do frontend falha (VITE_API_URL)

Confirme que o `docker-compose.prod.yml` tem o `args` correto:

```yaml
args:
  VITE_API_URL: /api
```

---

## Estrutura de arquivos de deploy

```
pethotelgo/                         ← backend
├── Dockerfile                      ← build da API Spring Boot
├── docker-compose.prod.yml         ← orquestra todos os serviços
├── .env                            ← variáveis (nunca versionar)
├── .env.example                    ← template com todos os campos
├── nginx/
│   ├── conf.d/
│   │   ├── default.conf            ← Nginx HTTP (proxy API + frontend)
│   │   └── ssl.conf.example        ← template HTTPS com Let's Encrypt
│   └── certbot/                    ← certificados SSL (gerado em runtime)
└── scripts/
    ├── setup-oracle.sh             ← setup inicial da VM
    └── deploy.sh                   ← build + deploy + healthcheck

pethotelgo-frontend/                ← frontend (diretório irmão)
├── Dockerfile                      ← build React → Nginx alpine
└── nginx.conf                      ← config do Nginx do frontend (SPA)
```
