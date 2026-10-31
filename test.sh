#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

RUN_BACKEND=true
RUN_FRONTEND=true
RUN_CONTRACT=false
API_URL="http://localhost:8080"
APP_URL="http://localhost:3000"
ADMIN_USER="admin"
ADMIN_PASS="Admin123!"
STAGE="E2"

usage() {
    echo "Użycie: $0 [opcje]"
    echo ""
    echo "Dostępne opcje:"
    echo "  --all              Uruchamia testy backendu, frontendu oraz testy kontraktowe (wymaga działającej aplikacji)"
    echo "  --backend          Uruchamia wyłącznie testy backendu (Maven)"
    echo "  --frontend         Uruchamia wyłącznie weryfikację frontendu (TypeScript + Vite build)"
    echo "  --contract         Uruchamia oficjalne testy kontraktowe prowadzącego (zai-tests.mjs)"
    echo "  --api <url>        Adres bazowy API dla testów kontraktowych (domyślnie: $API_URL)"
    echo "  --app <url>        Adres bazowy aplikacji dla testów kontraktowych (domyślnie: $APP_URL)"
    echo "  --user <nazwa>     Użytkownik administratora (domyślnie: $ADMIN_USER)"
    echo "  --pass <haslo>     Hasło administratora (domyślnie: $ADMIN_PASS)"
    echo "  --stage <E1|E2>    Etap testów prowadzącego (domyślnie: $STAGE)"
    echo "  -h, --help         Wyświetla tę pomoc"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --all)
            RUN_BACKEND=true
            RUN_FRONTEND=true
            RUN_CONTRACT=true
            shift
            ;;
        --backend)
            RUN_BACKEND=true
            RUN_FRONTEND=false
            RUN_CONTRACT=false
            shift
            ;;
        --frontend)
            RUN_BACKEND=false
            RUN_FRONTEND=true
            RUN_CONTRACT=false
            shift
            ;;
        --contract)
            RUN_BACKEND=false
            RUN_FRONTEND=false
            RUN_CONTRACT=true
            shift
            ;;
        --api)
            API_URL="$2"
            RUN_CONTRACT=true
            shift 2
            ;;
        --app)
            APP_URL="$2"
            RUN_CONTRACT=true
            shift 2
            ;;
        --user)
            ADMIN_USER="$2"
            shift 2
            ;;
        --pass)
            ADMIN_PASS="$2"
            shift 2
            ;;
        --stage)
            STAGE="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "Nieznana opcja: $1"
            usage
            ;;
    esac
done

if [ "$RUN_BACKEND" = true ]; then
    echo "=== [1/2] Testy backendu (Spring Boot / Maven) ==="
    cd "$DIR/backend"
    if [ -z "$JAVA_HOME" ] && [ -d "/usr/local/opt/openjdk@17" ]; then
        export JAVA_HOME="/usr/local/opt/openjdk@17"
        export PATH="$JAVA_HOME/bin:$PATH"
    fi
    ./mvnw test
    cd "$DIR"
    echo "✓ Testy backendu zakończone sukcesem."
    echo ""
fi

if [ "$RUN_FRONTEND" = true ]; then
    echo "=== [2/2] Testy frontendu (TypeScript & Vite build) ==="
    cd "$DIR/frontend"
    npm run build
    cd "$DIR"
    echo "✓ Weryfikacja frontendu zakończona sukcesem."
    echo ""
fi

if [ "$RUN_CONTRACT" = true ]; then
    echo "=== Testy kontraktowe i walidacyjne prowadzącego (zai-tests.mjs) ==="
    node "$DIR/zai-tests.mjs" --app "$APP_URL" --api "$API_URL" --user "$ADMIN_USER" --pass "$ADMIN_PASS" --stage "$STAGE"
    echo "✓ Testy kontraktowe zakończone pomyślnie."
fi
