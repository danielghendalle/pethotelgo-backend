#!/bin/bash
# Deploy / update PetHotelGO on Oracle Cloud.
# Run from the project root directory.
# Usage: bash scripts/deploy.sh [--ssl SEU_DOMINIO.com]

set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
SSL_DOMAIN=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --ssl) SSL_DOMAIN="$2"; shift 2 ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

# ── Sanity checks ──────────────────────────────────────────────────────────────
if [[ ! -f ".env" ]]; then
    echo "ERROR: .env file not found. Copy .env.example to .env and fill in the values."
    exit 1
fi

# Verify required env vars are set in .env
REQUIRED_VARS=(POSTGRES_USER POSTGRES_PASSWORD FIREBASE_CREDENTIALS_BASE64 CORS_ALLOWED_ORIGINS)
MISSING=()
for var in "${REQUIRED_VARS[@]}"; do
    val=$(grep -E "^${var}=" .env | cut -d= -f2- | tr -d '[:space:]')
    if [[ -z "$val" ]]; then
        MISSING+=("$var")
    fi
done
if [[ ${#MISSING[@]} -gt 0 ]]; then
    echo "ERROR: The following required variables are empty in .env:"
    for v in "${MISSING[@]}"; do echo "  - $v"; done
    exit 1
fi

# Warn if swap is not configured (critical for 1 GB RAM VMs)
SWAP_TOTAL=$(free -m | awk '/^Swap:/ {print $2}')
if [[ "$SWAP_TOTAL" -lt 512 ]]; then
    echo "WARNING: Swap is ${SWAP_TOTAL}MB. On a 1 GB RAM VM the Maven build may OOM."
    echo "         Run the following to add 2 GB swap before deploying:"
    echo "           sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile"
    echo "           sudo mkswap /swapfile && sudo swapon /swapfile"
    echo "           echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab"
    echo ""
fi

echo "=== PetHotelGO Deploy ==="

# ── Pull latest code ───────────────────────────────────────────────────────────
echo "[1/4] Pulling latest code..."
git pull --ff-only

# ── Build images ───────────────────────────────────────────────────────────────
# Build sequentially to avoid OOM on low-memory VMs (1 GB RAM).
# Maven + npm running in parallel easily exceeds available memory.
echo "[2/4] Building Docker images (this may take a few minutes)..."
echo "  Building backend (api)..."
docker compose -f "$COMPOSE_FILE" build --no-cache api
echo "  Building frontend..."
docker compose -f "$COMPOSE_FILE" build --no-cache frontend

# ── Start / restart containers ─────────────────────────────────────────────────
echo "[3/4] Starting containers..."
docker compose -f "$COMPOSE_FILE" up -d

# ── Health check ───────────────────────────────────────────────────────────────
echo "[4/4] Waiting for API to become healthy..."
MAX_WAIT=120
ELAPSED=0
until docker compose -f "$COMPOSE_FILE" exec -T api curl -sf http://localhost:8080/api/actuator/health > /dev/null 2>&1; do
    if [[ $ELAPSED -ge $MAX_WAIT ]]; then
        echo "ERROR: API did not become healthy within ${MAX_WAIT}s."
        echo "Check logs with: docker compose -f $COMPOSE_FILE logs api"
        exit 1
    fi
    printf "  waiting... (%ds)\n" "$ELAPSED"
    sleep 5
    ELAPSED=$((ELAPSED + 5))
done
echo "  API is healthy!"

# ── Optional: issue SSL certificate via Let's Encrypt ─────────────────────────
if [[ -n "$SSL_DOMAIN" ]]; then
    echo ""
    echo "=== SSL Setup for $SSL_DOMAIN ==="
    echo "Make sure DNS A record for $SSL_DOMAIN points to this server's public IP."
    read -rp "Continue with SSL certificate issuance? [y/N] " CONFIRM
    if [[ "$CONFIRM" =~ ^[Yy]$ ]]; then
        docker compose -f "$COMPOSE_FILE" run --rm certbot certonly \
            --webroot -w /var/www/certbot \
            --email "admin@${SSL_DOMAIN}" \
            --agree-tos --no-eff-email \
            -d "$SSL_DOMAIN"

        echo ""
        echo "Certificate issued! Now enable HTTPS:"
        echo "  1. cp nginx/conf.d/ssl.conf.example nginx/conf.d/ssl.conf"
        echo "  2. sed -i 's/SEU_DOMINIO.com/$SSL_DOMAIN/g' nginx/conf.d/ssl.conf"
        echo "  3. rm nginx/conf.d/default.conf"
        echo "  4. docker compose -f $COMPOSE_FILE restart nginx"
    fi
fi

# ── Cleanup old images ─────────────────────────────────────────────────────────
docker image prune -f > /dev/null 2>&1 || true

echo ""
echo "=== Deploy complete! ==="
echo "API running at: http://$(curl -s ifconfig.me)/api"
echo ""
echo "Useful commands:"
echo "  Logs:    docker compose -f $COMPOSE_FILE logs -f api"
echo "  Status:  docker compose -f $COMPOSE_FILE ps"
echo "  Stop:    docker compose -f $COMPOSE_FILE down"
