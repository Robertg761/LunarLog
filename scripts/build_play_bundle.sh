#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required_keys=(
  LL_SIGNING_STORE_FILE
  LL_SIGNING_STORE_PASSWORD
  LL_SIGNING_KEY_ALIAS
  LL_SIGNING_KEY_PASSWORD
)

has_signing_config=true
for key in "${required_keys[@]}"; do
  if [[ -n "${!key:-}" ]]; then
    continue
  fi
  if [[ -f "$ROOT_DIR/keystore.properties" ]] && grep -q "^${key}=" "$ROOT_DIR/keystore.properties"; then
    continue
  fi
  has_signing_config=false
done

if [[ "$has_signing_config" != "true" ]]; then
  echo "Missing Play signing configuration."
  echo "Populate keystore.properties or export LL_SIGNING_* variables before building."
  exit 1
fi

./gradlew :app:bundlePlayRelease

echo
echo "Play bundle ready:"
find "$ROOT_DIR/app/build/outputs/bundle/playRelease" -name '*.aab' -maxdepth 1 -print
