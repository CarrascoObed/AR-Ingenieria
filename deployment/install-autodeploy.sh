#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "$EUID" -ne 0 ]]; then
  echo "Ejecuta este instalador con sudo: sudo bash deployment/install-autodeploy.sh" >&2
  exit 1
fi

SERVICE_USER="${SUDO_USER:-}"
if [[ -z "$SERVICE_USER" || "$SERVICE_USER" == "root" ]]; then
  echo "No se pudo identificar el usuario propietario del clon. Ejecuta el instalador con sudo desde tu usuario normal." >&2
  exit 1
fi

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
USER_HOME="$(getent passwd "$SERVICE_USER" | cut -d: -f6)"

if [[ ! -f "$PROJECT_DIR/docker-compose.yml" || ! -d "$PROJECT_DIR/.git" ]]; then
  echo "Ejecuta este instalador dentro del clon de AR-Ingenieria." >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "Se requiere Docker Engine con el complemento Docker Compose." >&2
  exit 1
fi

for herramienta in git curl python3; do
  if ! command -v "$herramienta" >/dev/null 2>&1; then
    echo "Se requiere la herramienta $herramienta para el despliegue automático." >&2
    exit 1
  fi
done

if ! getent group docker >/dev/null 2>&1; then
  echo "No existe el grupo docker. Verifica la instalación de Docker." >&2
  exit 1
fi

usermod -aG docker "$SERVICE_USER"

cat > /etc/systemd/system/aring-autodeploy.service <<EOF
[Unit]
Description=Actualizar AR-Ingenieria desde GitHub y desplegar con Docker Compose
After=docker.service network-online.target
Wants=network-online.target
Requires=docker.service

[Service]
Type=oneshot
User=$SERVICE_USER
SupplementaryGroups=docker
Environment=HOME=$USER_HOME
Environment=AR_INGENIERIA_PROJECT_DIR=$PROJECT_DIR
EnvironmentFile=-/etc/aring-autodeploy.env
WorkingDirectory=$PROJECT_DIR
ExecStart=/usr/bin/bash $PROJECT_DIR/deployment/auto-deploy.sh
TimeoutStartSec=30min
EOF

cat > /etc/systemd/system/aring-autodeploy.timer <<'EOF'
[Unit]
Description=Revisar periódicamente actualizaciones de AR-Ingenieria en GitHub

[Timer]
OnBootSec=1min
OnUnitActiveSec=1min
AccuracySec=15s
Unit=aring-autodeploy.service

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable --now aring-autodeploy.timer

echo "Temporizador instalado para $PROJECT_DIR (rama main, revisión cada minuto)."
echo "Consulta: sudo systemctl status aring-autodeploy.timer"
echo "Registros: sudo journalctl -u aring-autodeploy.service -f"
