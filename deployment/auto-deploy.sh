#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="${AR_INGENIERIA_PROJECT_DIR:?AR_INGENIERIA_PROJECT_DIR is required}"
BRANCH="main"
COMPOSE_PROJECT="ar-ingenieria"

if [[ ! -d "$PROJECT_DIR/.git" ]]; then
  echo "No se encontró el repositorio Git en: $PROJECT_DIR" >&2
  exit 1
fi

if [[ -n "$(git -C "$PROJECT_DIR" status --porcelain)" ]]; then
  echo "Hay cambios locales en el repositorio; se cancela el despliegue automático." >&2
  exit 1
fi

git -C "$PROJECT_DIR" fetch --quiet origin "$BRANCH"
LOCAL_COMMIT="$(git -C "$PROJECT_DIR" rev-parse HEAD)"
REMOTE_COMMIT="$(git -C "$PROJECT_DIR" rev-parse "origin/$BRANCH")"

if [[ "$LOCAL_COMMIT" == "$REMOTE_COMMIT" ]]; then
  echo "Sin cambios nuevos en origin/$BRANCH."
  exit 0
fi

if ! git -C "$PROJECT_DIR" merge-base --is-ancestor "$LOCAL_COMMIT" "$REMOTE_COMMIT"; then
  echo "origin/$BRANCH no es avance directo de la versión local; se cancela el despliegue." >&2
  exit 1
fi

echo "Actualizando desde $LOCAL_COMMIT hasta $REMOTE_COMMIT."
git -C "$PROJECT_DIR" merge --ff-only "$REMOTE_COMMIT"

docker compose \
  --project-name "$COMPOSE_PROJECT" \
  --project-directory "$PROJECT_DIR" \
  --file "$PROJECT_DIR/docker-compose.yml" \
  up -d --build

echo "Despliegue completado en $(git -C "$PROJECT_DIR" rev-parse --short HEAD)."
