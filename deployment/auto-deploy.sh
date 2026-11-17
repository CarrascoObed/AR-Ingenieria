#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="${AR_INGENIERIA_PROJECT_DIR:?AR_INGENIERIA_PROJECT_DIR is required}"
BRANCH="main"
COMPOSE_PROJECT="ar-ingenieria"
GITHUB_REPOSITORY="${AR_INGENIERIA_GITHUB_REPOSITORY:-CarrascoObed/AR-Ingenieria}"
CI_WORKFLOW="ci.yml"

ci_state_for_commit() {
  local commit="$1"
  local api_url="https://api.github.com/repos/$GITHUB_REPOSITORY/actions/workflows/$CI_WORKFLOW/runs?branch=$BRANCH&event=push&head_sha=$commit&per_page=20"
  local headers=(
    -H "Accept: application/vnd.github+json"
    -H "X-GitHub-Api-Version: 2022-11-28"
  )

  if [[ -n "${GITHUB_TOKEN:-}" ]]; then
    headers+=(-H "Authorization: Bearer $GITHUB_TOKEN")
  fi

  curl --fail --silent --show-error --retry 2 --max-time 20 \
    "${headers[@]}" "$api_url" |
    python3 -c '
import json
import sys

commit = sys.argv[1]
data = json.load(sys.stdin)
runs = [run for run in data.get("workflow_runs", []) if run.get("head_sha") == commit and run.get("event") == "push"]
if not runs:
    print("missing")
else:
    run = max(runs, key=lambda item: (item.get("run_number", 0), item.get("run_attempt", 0), item.get("id", 0)))
    status = run.get("status", "unknown")
    conclusion = run.get("conclusion") or ""
    print("success" if status == "completed" and conclusion == "success" else
          "failure" if status == "completed" else "pending")
' "$commit"
}

if [[ ! -d "$PROJECT_DIR/.git" ]]; then
  echo "No se encontró el repositorio Git en: $PROJECT_DIR" >&2
  exit 1
fi

if [[ -n "$(git -C "$PROJECT_DIR" status --porcelain)" ]]; then
  echo "Hay cambios locales en el repositorio; se cancela el despliegue automático." >&2
  exit 1
fi

git -C "$PROJECT_DIR" fetch --quiet origin "refs/heads/$BRANCH"
LOCAL_COMMIT="$(git -C "$PROJECT_DIR" rev-parse HEAD)"
REMOTE_COMMIT="$(git -C "$PROJECT_DIR" rev-parse FETCH_HEAD)"
DEPLOY_STATE_FILE="$PROJECT_DIR/.git/ar-autodeploy-deployed-sha"
DEPLOYED_COMMIT="$(cat "$DEPLOY_STATE_FILE" 2>/dev/null || true)"

if [[ "$LOCAL_COMMIT" == "$REMOTE_COMMIT" && "$REMOTE_COMMIT" == "$DEPLOYED_COMMIT" ]]; then
  echo "origin/$BRANCH ya está desplegada en $REMOTE_COMMIT."
  exit 0
fi

if [[ "$LOCAL_COMMIT" != "$REMOTE_COMMIT" ]]; then
  if ! git -C "$PROJECT_DIR" merge-base --is-ancestor "$LOCAL_COMMIT" "$REMOTE_COMMIT"; then
    echo "origin/$BRANCH no es avance directo de la versión local; se cancela el despliegue." >&2
    exit 1
  fi
fi

echo "Esperando una ejecución correcta de CI para $REMOTE_COMMIT."
if ! CI_STATE="$(ci_state_for_commit "$REMOTE_COMMIT")"; then
  echo "No se pudo consultar GitHub Actions; se cancela el despliegue por seguridad." >&2
  exit 1
fi

case "$CI_STATE" in
  success)
    echo "CI aprobó $REMOTE_COMMIT."
    ;;
  pending)
    echo "CI sigue ejecutándose; se volverá a revisar en el próximo ciclo."
    exit 0
    ;;
  failure)
    echo "CI falló para $REMOTE_COMMIT; no se desplegará." >&2
    exit 1
    ;;
  missing)
    echo "GitHub Actions aún no tiene una ejecución CI para $REMOTE_COMMIT; se volverá a revisar." >&2
    exit 0
    ;;
  *)
    echo "Estado CI desconocido para $REMOTE_COMMIT: $CI_STATE" >&2
    exit 1
    ;;
esac

echo "Actualizando desde $LOCAL_COMMIT hasta $REMOTE_COMMIT."
if [[ "$LOCAL_COMMIT" != "$REMOTE_COMMIT" ]]; then
  git -C "$PROJECT_DIR" merge --ff-only "$REMOTE_COMMIT"
fi

docker compose \
  --project-name "$COMPOSE_PROJECT" \
  --project-directory "$PROJECT_DIR" \
  --file "$PROJECT_DIR/docker-compose.yml" \
  up -d --build

printf '%s\n' "$REMOTE_COMMIT" > "$DEPLOY_STATE_FILE.tmp"
mv "$DEPLOY_STATE_FILE.tmp" "$DEPLOY_STATE_FILE"

echo "Despliegue completado en $(git -C "$PROJECT_DIR" rev-parse --short HEAD)."
