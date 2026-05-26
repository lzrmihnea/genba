#!/usr/bin/env bash
# Genba — local dev startup
# Brings up PostgreSQL via Docker; backend + frontend run natively for fast iteration.

set -euo pipefail

cd "$(dirname "$0")"

if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker is not running. Start Docker Desktop and try again."
    exit 1
fi

DOCKER_COMPOSE="docker compose"
if ! docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE="docker-compose"
fi

echo "🐘 Starting PostgreSQL on port 5435..."
$DOCKER_COMPOSE up -d postgres

echo "⏳ Waiting for PostgreSQL to be healthy..."
until $DOCKER_COMPOSE exec -T postgres pg_isready -U genba_user -d genba_db >/dev/null 2>&1; do
    sleep 1
done
echo "✅ PostgreSQL is ready at localhost:5435 (db genba_db, user genba_user)"
echo ""
echo "Next steps — in two separate terminals:"
echo ""
echo "  Backend:   cd genba-backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "             → http://localhost:8086"
echo "             → Swagger: http://localhost:8086/swagger-ui/index.html"
echo ""
echo "  Frontend:  cd genba-frontend && npm install && npm run dev"
echo "             → http://localhost:3001"
echo ""
echo "Stop postgres:  $DOCKER_COMPOSE down"
echo "Wipe DB:        $DOCKER_COMPOSE down -v"
