#!/bin/bash
# Initial setup for Oracle Cloud Always Free instance (Ubuntu 22.04)
# Run once as the ubuntu user after SSH into the instance.
# Usage: bash setup-oracle.sh

set -euo pipefail

echo "=== PetHotelGO - Oracle Cloud Setup ==="

# ── System update ──────────────────────────────────────────────────────────────
echo "[1/5] Updating system packages..."
sudo apt-get update -qq && sudo apt-get upgrade -y -qq

# ── Docker ─────────────────────────────────────────────────────────────────────
echo "[2/5] Installing Docker..."
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"

# ── Utilities ──────────────────────────────────────────────────────────────────
echo "[3/5] Installing utilities..."
sudo apt-get install -y -qq git curl wget iptables-persistent

# ── Firewall: open ports 80 and 443 ───────────────────────────────────────────
# Oracle Cloud Ubuntu instances use iptables by default.
# You also need to open these ports in the OCI Console:
#   Networking > VCN > Security Lists > Add Ingress Rules for TCP 80 and 443
echo "[4/5] Configuring iptables (ports 80 and 443)..."
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80  -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save

# ── Clone repositories ─────────────────────────────────────────────────────────
echo "[5/5] Cloning repositories..."
read -rp "Backend repo URL (e.g. https://github.com/user/pethotelgo.git): "         BACKEND_URL
read -rp "Frontend repo URL (e.g. https://github.com/user/pethotelgo-frontend.git): " FRONTEND_URL

git clone "$BACKEND_URL"  pethotelgo
git clone "$FRONTEND_URL" pethotelgo-frontend

cd pethotelgo

echo ""
echo "=== Setup complete! ==="
echo ""
echo "NEXT STEPS:"
echo "  1. cd pethotelgo"
echo "  2. cp .env.example .env"
echo "  3. nano .env          # fill in all values (JWT_SECRET, passwords, Firebase base64, etc.)"
echo "  4. bash scripts/deploy.sh"
echo ""
echo "Directory layout expected by docker-compose.prod.yml:"
echo "  ~/pethotelgo/           <- backend (docker-compose.prod.yml is here)"
echo "  ~/pethotelgo-frontend/  <- frontend (built by compose via ../pethotelgo-frontend)"
echo ""
echo "NOTE: Log out and back in (or run 'newgrp docker') to use Docker without sudo."
