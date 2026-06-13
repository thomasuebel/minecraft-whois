#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# release.sh — build a release candidate zip for the Whois Paper plugin
#
# Output: release/Whois-<version>.zip
#   ├── Whois-<version>.jar   (drop into plugins/)
#   └── SPEC.md
# ---------------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# --- Build ---
echo "Building plugin jar..."
set +u; source "$HOME/.sdkman/bin/sdkman-init.sh" 2>/dev/null || true; set -u
./gradlew build --quiet

# --- Determine version from the built jar ---
JAR=$(ls build/libs/Whois-*.jar | head -1)
FILENAME=$(basename "$JAR")                   # Whois-0.1.0.jar
VERSION="${FILENAME#Whois-}"                  # 0.1.0.jar
VERSION="${VERSION%.jar}"                     # 0.1.0

RELEASE_NAME="Whois-${VERSION}"
RELEASE_DIR="release/${RELEASE_NAME}"
ZIP_PATH="release/${RELEASE_NAME}.zip"

# --- Stage files ---
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"

cp "$JAR"     "$RELEASE_DIR/${RELEASE_NAME}.jar"
cp SPEC.md    "$RELEASE_DIR/SPEC.md"

# --- Zip ---
rm -f "$ZIP_PATH"
(cd release && zip -r "$(basename "$ZIP_PATH")" "$(basename "$RELEASE_DIR")")
rm -rf "$RELEASE_DIR"

echo ""
echo "Release candidate ready: $ZIP_PATH"
echo ""
unzip -l "$ZIP_PATH"
