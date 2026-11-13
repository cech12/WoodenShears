#!/usr/bin/env bash
# Server start test: starts a real dedicated server (Docker image itzg/minecraft-server) with the built release jar
# of one loader and checks that it starts cleanly. Used by the CI (MinecraftModActions server-starttest.yml) and
# can be run locally in the same way:
#
#   ./gradlew build
#   scripts/server-starttest.sh <fabric|forge|neoforge> [--with-integrations] [--print-cache-key]
#
# --with-integrations  also install the optional mods of scripts/server-starttest.deps. If the loader has no optional
#                      mods, there is nothing to test beyond the run without this option and the script exits with 0.
# --print-cache-key    only print the cache key of the server installation in $STARTTEST_DIR/<loader>-<mode>/data
#                      (empty if the run is skipped, see above) and exit. Used by the CI.
#
# Requirements: bash, docker, curl, jq
# All versions come from gradle.properties, additional mods from scripts/server-starttest.deps.
#
# Environment variables (optional):
#   STARTTEST_TIMEOUT   seconds to wait for a healthy server (default 600)
#   STARTTEST_DIR       working directory for server data and logs (default .server-starttest)
#   STARTTEST_IMAGE     docker image (default itzg/minecraft-server:java<java_version>)
set -euo pipefail

usage() { echo "Usage: $0 <fabric|forge|neoforge> [--with-integrations] [--print-cache-key]" >&2; exit 2; }
LOADER="${1:-}"
[ $# -gt 0 ] && shift
MODE="required"
PRINT_CACHE_KEY=0
for arg in "$@"; do
    case "$arg" in
        --with-integrations) MODE="integrations" ;;
        --print-cache-key) PRINT_CACHE_KEY=1 ;;
        *) echo "Unknown option: $arg" >&2; usage ;;
    esac
done
case "$LOADER" in
    fabric)   LOADER_TYPE=FABRIC;   LOADER_VERSION_KEY=fabric_loader_version; LOADER_VERSION_ENV=FABRIC_LOADER_VERSION ;;
    forge)    LOADER_TYPE=FORGE;    LOADER_VERSION_KEY=forge_version;         LOADER_VERSION_ENV=FORGE_VERSION ;;
    neoforge) LOADER_TYPE=NEOFORGE; LOADER_VERSION_KEY=neoforge_version;      LOADER_VERSION_ENV=NEOFORGE_VERSION ;;
    *) usage ;;
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
LOADER_VERSION="$(prop "$LOADER_VERSION_KEY")"
LOADER_ENV=(-e "TYPE=$LOADER_TYPE" -e "$LOADER_VERSION_ENV=$LOADER_VERSION")

# The integrations run only makes sense if the loader has optional mods, otherwise it equals the required run.
SKIP=0
if [ "$MODE" = "integrations" ] && ! grep -qE "^[[:space:]]*${LOADER}[[:space:]]+optional[[:space:]]" scripts/server-starttest.deps 2>/dev/null; then
    SKIP=1
fi

if [ "$PRINT_CACHE_KEY" -eq 1 ]; then
    [ "$SKIP" -eq 0 ] && echo "server-starttest-${LOADER}-${MODE}-${MC_VERSION}-${LOADER_VERSION}-java${JAVA_VERSION}"
    exit 0
fi
if [ "$SKIP" -eq 1 ]; then
    mkdir -p "$WORK"
    printf '### Server start test: %s (%s)\n\nSkipped: no optional mods for %s in scripts/server-starttest.deps, this run would equal the run without integrations.\n' \
        "$LOADER" "$MODE" "$LOADER" | tee "$WORK/summary.md"
    exit 0
fi

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
        # a failed request is an error, only an empty result means "not available"
        if ! versions="$(curl -fsS --max-time 30 --retry 3 --retry-all-errors \
            -A "server-starttest (github.com/cech12/MinecraftModActions)" -G \
            "https://api.modrinth.com/v2/project/${dep_slug}/version" \
            --data-urlencode "loaders=[\"${LOADER}\"]" \
            --data-urlencode "game_versions=[\"${MC_VERSION}\"]")"; then
            log "- **Modrinth request for $dep_slug failed** (network or API error, see above)"
            exit 1
        fi
        # exact version number first, then with build metadata (e.g. 26.3.158+fabric), then with a "-" suffix
        # (e.g. 20.0.149-fabric) only if it is a release, so that a pre-release like 1.2-beta is never picked for 1.2
        version_id="$(jq -r --arg v "$dep_version" \
            '(map(select(.version_number == $v))
              + map(select(.version_number | startswith($v + "+")))
              + map(select((.version_number | startswith($v + "-")) and .version_type == "release")))[0].id // empty' <<< "$versions")"
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
