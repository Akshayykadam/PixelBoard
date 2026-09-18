#!/usr/bin/env bash
# ==============================================================================
# PixelBoard - Build & Patch Automation Script
# Maintainer: Akshay Kadam (@Akshayykadam)
# Repository: https://github.com/Akshayykadam/PixelBoard
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

REBUILD_PATCH=false
for arg in "$@"; do
    case "$arg" in
        -r|--rebuild|--compile|-c)
            REBUILD_PATCH=true
            ;;
        -h|--help)
            echo "Usage: ./build.sh [options]"
            echo "Options:"
            echo "  -r, --rebuild    Recompile patch bundle from pixelboard-patches/ using Gradle"
            echo "  -h, --help       Show this help message"
            echo ""
            echo "By default, ./build.sh runs 100% offline using the pre-bundled patches/PixelBoard.mpp."
            exit 0
            ;;
    esac
done

MPP_PATH="patches/PixelBoard.mpp"

if [[ "$REBUILD_PATCH" == true ]] || [[ ! -f "$MPP_PATH" ]]; then
    echo "=== 🎹 Compiling PixelBoard Patch Bundle from Source ==="
    cd pixelboard-patches
    ./gradlew :patches:buildAndroid
    cd "$SCRIPT_DIR"

    PATCH_VERSION=$(grep -E "^version\s*=" pixelboard-patches/gradle.properties | cut -d'=' -f2 | tr -d ' ')
    COMPILED_MPP="pixelboard-patches/patches/build/libs/patches-${PATCH_VERSION}.mpp"
    if [[ ! -f "$COMPILED_MPP" ]]; then
        COMPILED_MPP=$(ls pixelboard-patches/patches/build/libs/patches-*.mpp 2>/dev/null | grep -v 'javadoc' | grep -v 'sources' | head -n 1)
    fi
    if [[ -z "${COMPILED_MPP:-}" ]] || [[ ! -f "$COMPILED_MPP" ]]; then
        echo "❌ Error: Patch bundle failed to build at pixelboard-patches/patches/build/libs/"
        exit 1
    fi
    mkdir -p patches
    cp "$COMPILED_MPP" "$MPP_PATH"
    echo "✅ Patch bundle compiled & synced to: $MPP_PATH"
else
    echo "📦 Using local patch bundle: $MPP_PATH (100% offline)"
fi

# Check for Java 21 to run patcher.jar
JAVA_BIN=""
for candidate in \
    "${JAVA_HOME:-}/bin/java" \
    "/opt/homebrew/opt/openjdk@21/bin/java" \
    /opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home/bin/java \
    "/opt/homebrew/opt/openjdk/bin/java" \
    "$(/usr/libexec/java_home -v 21 2>/dev/null)/bin/java" \
    "$(which java 2>/dev/null)"; do
    if [[ -x "$candidate" ]]; then
        candidate_ver=$("$candidate" -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
        if [[ "$candidate_ver" -ge 21 ]]; then
            JAVA_BIN="$candidate"
            break
        fi
    fi
done

if [[ -z "$JAVA_BIN" ]]; then
    echo "⚠️ Note: patcher.jar requires Java 21+."
    echo "   Please install Java 21 (e.g. 'brew install openjdk@21') or specify JAVA_HOME."
    exit 1
fi
echo "☕ Using Java 21+: $JAVA_BIN"

if [[ ! -f "input/gboard.apk" ]]; then
    echo "⚠️ Note: Place stock Gboard APK at input/gboard.apk to generate output APK."
    exit 0
fi

echo "=== 🔨 Applying Patches to input/gboard.apk ==="
mkdir -p output
mkdir -p tools/patcher-data

KEYSTORE_PATH="tools/patcher-data/pixelboard.keystore"
if [[ ! -f "$KEYSTORE_PATH" ]]; then
    echo "🔑 Generating PixelBoard release keystore..."
    keytool -genkeypair \
        -keystore "$KEYSTORE_PATH" \
        -alias pixelboard \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -storepass pixelboard \
        -keypass pixelboard \
        -dname "CN=PixelBoard, OU=PixelBoard, O=PixelBoard, L=Unknown, ST=Unknown, C=US" \
        >/dev/null 2>&1
fi

"$JAVA_BIN" -jar tools/patcher.jar patch \
    --patches="$MPP_PATH" \
    --options-file=options.json \
    --striplibs=arm64-v8a \
    --signer="PixelBoard" \
    --keystore="$KEYSTORE_PATH" \
    --keystore-password=pixelboard \
    --keystore-entry-alias=pixelboard \
    --keystore-entry-password=pixelboard \
    --temporary-files-path="tools/patcher-data/tmp" \
    -o=output/PixelBoard.apk \
    -r=output/patching-result.json \
    input/gboard.apk

echo "🎉 Successfully built output/PixelBoard.apk!"
