#!/usr/bin/env bash
set -euo pipefail

version="1.21.11"
folia_commit="3ef0ba66b20599d24f235ac795865047c29c5eb4"
root_dir="$(cd "$(dirname "$0")/.." && pwd)"
target="$root_dir/libs/folia-server-dev-${version}.jar"

mkdir -p "$(dirname "$target")"

if [[ -f "$target" ]]; then
    echo "Using existing $target"
    exit 0
fi

source_dir="${FOLIA_SOURCE_DIR:-${TMPDIR:-/tmp}/folia-carpet-folia-${version}-$folia_commit}"
if [[ ! -d "$source_dir/.git" ]]; then
    git clone --filter=blob:none https://github.com/PaperMC/Folia.git "$source_dir"
fi
git -C "$source_dir" fetch --depth 1 origin "$folia_commit"
git -C "$source_dir" checkout --detach "$folia_commit"

if [[ ! -f "$source_dir/folia-server/build/libs/folia-server-${version}-R0.1-SNAPSHOT.jar" ]]; then
    (cd "$source_dir" && ./gradlew applyAllPatches && ./gradlew :folia-server:build --no-daemon)
fi

cp "$source_dir/folia-server/build/libs/folia-server-${version}-R0.1-SNAPSHOT.jar" "$target"
echo "Built $target from Folia commit $folia_commit"
