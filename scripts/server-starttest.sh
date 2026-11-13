#!/usr/bin/env bash
# Server start test: starts a real dedicated server (Docker image itzg/minecraft-server) with the built release jar
# of one loader and checks that it starts cleanly. Used by the CI (MinecraftModActions server-starttest.yml) and
# can be run locally in the same way:
#
#   ./gradlew build
#   scripts/server-starttest.sh <fabric|forge|neoforge> [--with-integrations]
#
# Requirements: bash, docker, curl, jq
# All versions come from gradle.properties, additional mods from scripts/server-starttest.deps.
#
# Environment variables (optional):
#   STARTTEST_TIMEOUT   seconds to wait for a healthy server (default 600)
#   STARTTEST_DIR       working directory for server data and logs (default .server-starttest)
#   STARTTEST_IMAGE     docker image (default itzg/minecraft-server:java<java_version>)
set -euo pipefail

LOADER="${1:-}"
MODE="required"
if [ "${2:-}" = "--with-integrations" ]; then
    MODE="integrations"
elif [ -n "${2:-}" ]; then
    echo "Unknown option: $2" >&2
    exit 2
fi
case "$LOADER" in
    fabric|forge|neoforge) ;;
    *) echo "Usage: $0 <fabric|forge|neoforge> [--with-integrations]" >&2; exit 2 ;;
esac

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

prop() {
    local value
    value="$(grep -E "^$1=" gradle.properties | head -1 | cut -d= -f2- | tr -d '\r')"
    if [ -z "$value" ]; then
        echo "Property $1 is missing in gradle.properties" >&2
        exit 2
    fi
    printf '%s' "$value"
}

MOD_ID="$(prop mod_id)"
MOD_VERSION="$(prop mod_version)"
MC_VERSION="$(prop minecraft_version)"
JAVA_VERSION="$(prop java_version)"
IMAGE="${STARTTEST_IMAGE:-itzg/minecraft-server:java${JAVA_VERSION}}"
TIMEOUT="${STARTTEST_TIMEOUT:-600}"
WORK="${STARTTEST_DIR:-$ROOT/.server-starttest}/${LOADER}-${MODE}"
NAME="${MOD_ID}-starttest-${LOADER}-${MODE}"

case "$LOADER" in
    fabric)   LOADER_ENV=(-e TYPE=FABRIC -e "FABRIC_LOADER_VERSION=$(prop fabric_loader_version)") ;;
    forge)    LOADER_ENV=(-e TYPE=FORGE -e "FORGE_VERSION=$(prop forge_version)") ;;
    neoforge) LOADER_ENV=(-e TYPE=NEOFORGE -e "NEOFORGE_VERSION=$(prop neoforge_version)") ;;
esac

JAR="$LOADER/build/libs/${MOD_ID}-${LOADER}-${MC_VERSION}-${MOD_VERSION}.jar"
if [ ! -f "$JAR" ]; then
    echo "Release jar $JAR not found. Run ./gradlew build first." >&2
    exit 2
fi

SUMMARY="$WORK/summary.md"
mkdir -p "$WORK/data" "$WORK/mods" "$WORK/logs"
# keep the cached server installation (libraries, server jar), but start every run with a clean mod list and world
rm -rf "$WORK/mods/"* "$WORK/logs/"* "$WORK/data/mods" "$WORK/data/config" "$WORK/data/world" \
       "$WORK/data/logs" "$WORK/data/crash-reports" "$WORK/data/.modrinth-manifest.json"
cp "$JAR" "$WORK/mods/"
: > "$SUMMARY"

log() { echo "$*"; echo "$*" >> "$SUMMARY"; }

log "### Server start test: $LOADER ($MODE)"
log ""
log "- Minecraft $MC_VERSION, ${LOADER_ENV[*]//-e /}, image \`$IMAGE\`"
log "- Mod: \`$(basename "$JAR")\`"

# Resolve additional mods from scripts/server-starttest.deps to Modrinth version ids.
MODRINTH_IDS=()
SKIPPED=()
if [ -f scripts/server-starttest.deps ]; then
    while read -r dep_loader dep_kind dep_slug dep_key; do
        [ -z "${dep_loader:-}" ] && continue
        case "$dep_loader" in \#*) continue ;; esac
        [ "$dep_loader" != "$LOADER" ] && continue
        [ "$dep_kind" = "optional" ] && [ "$MODE" != "integrations" ] && continue
        dep_version="$(prop "$dep_key")"
        versions="$(curl -fsS -A "server-starttest (github.com/cech12/MinecraftModActions)" -G \
            "https://api.modrinth.com/v2/project/${dep_slug}/version" \
            --data-urlencode "loaders=[\"${LOADER}\"]" \
            --data-urlencode "game_versions=[\"${MC_VERSION}\"]" || echo '[]')"
        # exact version number, or the version number with a loader suffix (e.g. 26.3.158+fabric)
        version_id="$(jq -r --arg v "$dep_version" \
            '[.[] | select(.version_number == $v or (.version_number | startswith($v + "+")) or (.version_number | startswith($v + "-")))][0].id // empty' <<< "$versions")"
        if [ -n "$version_id" ]; then
            MODRINTH_IDS+=("${dep_slug}:${version_id}")
            log "- $dep_kind: $dep_slug $dep_version"
        elif [ "$dep_kind" = "required" ]; then
            log "- **required mod $dep_slug $dep_version is not available on Modrinth for $LOADER $MC_VERSION**"
            exit 1
        else
            SKIPPED+=("$dep_slug $dep_version")
            log "- skipped (not available for $LOADER $MC_VERSION): $dep_slug $dep_version"
        fi
    done < scripts/server-starttest.deps
fi
log ""

MODRINTH_PROJECTS="$(IFS=,; echo "${MODRINTH_IDS[*]:-}")"

cleanup() { docker rm -f "$NAME" > /dev/null 2>&1 || true; }
cleanup
trap cleanup EXIT

docker run -d --name "$NAME" \
    -e EULA=TRUE \
    -e VERSION="$MC_VERSION" \
    "${LOADER_ENV[@]}" \
    -e MODRINTH_PROJECTS="$MODRINTH_PROJECTS" \
    -e MEMORY=2G \
    -e UID="$(id -u)" -e GID="$(id -g)" \
    -e ONLINE_MODE=FALSE \
    -v "$WORK/data:/data" \
    -v "$WORK/mods:/mods:ro" \
    "$IMAGE" > /dev/null

echo "Waiting up to ${TIMEOUT}s for the server to become healthy..."
STATUS="starting"
START=$(date +%s)
while true; do
    STATE="$(docker inspect -f '{{.State.Status}}' "$NAME" 2>/dev/null || echo missing)"
    STATUS="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{end}}' "$NAME" 2>/dev/null || echo missing)"
    if [ "$STATUS" = "healthy" ] || [ "$STATE" != "running" ]; then
        break
    fi
    if [ $(( $(date +%s) - START )) -ge "$TIMEOUT" ]; then
        STATUS="timeout"
        break
    fi
    sleep 5
done
echo "Server state: $STATE, health: $STATUS after $(( $(date +%s) - START ))s"

# stop gracefully (SIGTERM is forwarded to the server, which saves and shuts down)
docker stop -t 120 "$NAME" > /dev/null 2>&1 || true
docker logs "$NAME" > "$WORK/logs/container.log" 2>&1 || true
cp -r "$WORK/data/logs/." "$WORK/logs/" 2>/dev/null || true
cp -r "$WORK/data/crash-reports" "$WORK/logs/" 2>/dev/null || true

FAILED=0
if [ "$STATUS" != "healthy" ]; then
    log "**Server did not become healthy (state: $STATE, health: $STATUS).**"
    FAILED=1
fi

# Problems in the server log. Known harmless lines can be excluded in scripts/server-starttest.allowlist (one regex per line).
SERVER_LOG="$WORK/logs/container.log"
PROBLEMS="$WORK/logs/problems.log"
# (crashes are detected via the crash-reports directory below, "crash" alone matches mod names like fabric-crash-report-info)
grep -inE "exception|mixin.*(error|fail)|error.*${MOD_ID}|${MOD_ID}.*error" "$SERVER_LOG" > "$PROBLEMS" || true
if [ -s "$PROBLEMS" ] && [ -f scripts/server-starttest.allowlist ]; then
    grep -vEf <(grep -vE '^\s*(#|$)' scripts/server-starttest.allowlist) "$PROBLEMS" > "$PROBLEMS.tmp" || true
    mv "$PROBLEMS.tmp" "$PROBLEMS"
fi
if [ -s "$PROBLEMS" ]; then
    log "**Server log contains $(wc -l < "$PROBLEMS") problem line(s):**"
    log ""
    log '```'
    head -n 40 "$PROBLEMS" >> "$SUMMARY"
    head -n 40 "$PROBLEMS"
    log '```'
    FAILED=1
fi
if ! grep -qiE "Done \([0-9.]+s\)!" "$SERVER_LOG"; then
    log "**Server log does not contain the \"Done\" message.**"
    FAILED=1
fi
if [ -d "$WORK/logs/crash-reports" ]; then
    log "**Crash report(s) found:** $(ls "$WORK/logs/crash-reports" | tr '\n' ' ')"
    FAILED=1
fi

if [ "$FAILED" -ne 0 ]; then
    log ""
    log "Last lines of the server log:"
    log '```'
    tail -n 60 "$SERVER_LOG" | tee -a "$SUMMARY"
    log '```'
    echo "Server start test FAILED. Logs: $WORK/logs"
    exit 1
fi
log "Server started cleanly. ✅"
if [ "${#SKIPPED[@]}" -gt 0 ]; then
    log ""
    log "Skipped integrations: ${SKIPPED[*]}"
fi
