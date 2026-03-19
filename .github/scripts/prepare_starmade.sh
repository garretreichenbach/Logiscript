#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${STARMADE_DEV_BASE_URL:-https://files-origin.star-made.org/build/dev/}"
DIRECT_URL="${STARMADE_BUILD_URL:-}"
DOWNLOAD_PATTERN="${STARMADE_DOWNLOAD_PATTERN:-\.zip$}"
WORK_ROOT="${WORK_ROOT:-$PWD/.ci/starmade}"
STAGE_ROOT="$WORK_ROOT/stage"
ARCHIVE_PATH="$WORK_ROOT/starmade_build.zip"

mkdir -p "$WORK_ROOT" "$STAGE_ROOT"

join_url() {
  local base="$1"
  local tail="$2"
  if [[ "$tail" =~ ^https?:// ]]; then
    printf '%s\n' "$tail"
    return
  fi
  if [[ "$base" != */ ]]; then
    base="$base/"
  fi
  printf '%s%s\n' "$base" "$tail"
}

extract_links() {
  sed -nE 's/.*href="([^"]+)".*/\1/p'
}

discover_latest_subdir() {
  local url="$1"
  curl -fsSL "$url" \
    | extract_links \
    | grep -E '/$' \
    | grep -Ev '^\.\.?/$' \
    | sed 's#/$##' \
    | sort -V \
    | tail -n 1
}

discover_zip_links() {
  local url="$1"
  curl -fsSL "$url" \
    | extract_links \
    | grep -E "$DOWNLOAD_PATTERN"
}

choose_newest_url() {
  awk -F'/' '
    {
      file=$NF
      print file "\t" $0
    }
  ' | sort -V | tail -n 1 | cut -f2-
}

resolve_download_url() {
  if [[ -n "$DIRECT_URL" ]]; then
    printf '%s\n' "$DIRECT_URL"
    return
  fi

  local candidates=""
  local root_links
  root_links="$(discover_zip_links "$BASE_URL" || true)"
  if [[ -n "$root_links" ]]; then
    while IFS= read -r link; do
      [[ -z "$link" ]] && continue
      candidates+="$(join_url "$BASE_URL" "$link")"$'\n'
    done <<< "$root_links"
  fi

  local subdirs
  subdirs="$(curl -fsSL "$BASE_URL" | extract_links | grep -E '/$' | grep -Ev '^\.\.?/$' | sed 's#/$##' | sort -V || true)"
  if [[ -n "$subdirs" ]]; then
    while IFS= read -r subdir; do
      [[ -z "$subdir" ]] && continue
      local subdir_url
      subdir_url="$(join_url "$BASE_URL" "$subdir/")"
      local sub_links
      sub_links="$(discover_zip_links "$subdir_url" || true)"
      if [[ -n "$sub_links" ]]; then
        while IFS= read -r link; do
          [[ -z "$link" ]] && continue
          candidates+="$(join_url "$subdir_url" "$link")"$'\n'
        done <<< "$sub_links"
      fi
    done <<< "$subdirs"
  fi

  local newest
  newest="$(printf '%s' "$candidates" | sed '/^$/d' | choose_newest_url || true)"
  if [[ -n "$newest" ]]; then
    printf '%s\n' "$newest"
    return
  fi

  echo "Failed to discover a StarMade build zip from $BASE_URL" >&2
  exit 1
}

DOWNLOAD_URL="$(resolve_download_url)"
echo "Resolved StarMade build download URL: $DOWNLOAD_URL"

curl -fL "$DOWNLOAD_URL" -o "$ARCHIVE_PATH"
unzip -q -o "$ARCHIVE_PATH" -d "$STAGE_ROOT"

STARMADE_JAR_PATH="$(find "$STAGE_ROOT" -type f -name 'StarMade.jar' | head -n 1 || true)"
if [[ -z "$STARMADE_JAR_PATH" ]]; then
  echo "Could not locate StarMade.jar in downloaded build archive" >&2
  exit 1
fi

STARMADE_BASE_DIR="$(dirname "$STARMADE_JAR_PATH")"
STARMADE_LIB_DIR="$STARMADE_BASE_DIR/lib"

if [[ ! -d "$STARMADE_LIB_DIR" ]]; then
  echo "Could not locate lib directory beside StarMade.jar" >&2
  exit 1
fi

FINAL_ROOT="$WORK_ROOT/StarMade"
FINAL_LIB="$FINAL_ROOT/lib"
mkdir -p "$FINAL_LIB" "$FINAL_ROOT/mods"

cp "$STARMADE_JAR_PATH" "$FINAL_ROOT/StarMade.jar"
cp -R "$STARMADE_LIB_DIR"/. "$FINAL_LIB"/

# Ensure trailing slash for Gradle property compatibility.
FINAL_ROOT_WITH_SLASH="$FINAL_ROOT/"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  {
    echo "download_url=$DOWNLOAD_URL"
    echo "starmade_root=$FINAL_ROOT_WITH_SLASH"
  } >> "$GITHUB_OUTPUT"
else
  echo "download_url=$DOWNLOAD_URL"
  echo "starmade_root=$FINAL_ROOT_WITH_SLASH"
fi
