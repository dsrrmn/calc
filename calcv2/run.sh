#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

# 1. Compile if classes do not exist or if sources are newer
NEEDS_BUILD=0
CALC_CLASS="$PROJECT_DIR/target/classes/com/calculator/app/Calculator.class"

if [ ! -f "$CALC_CLASS" ]; then
    NEEDS_BUILD=1
elif [ -n "$(find "$PROJECT_DIR/src/main/java" -type f -newer "$CALC_CLASS" -print -quit 2>/dev/null)" ]; then
    NEEDS_BUILD=1
fi

if [ "$NEEDS_BUILD" -eq 1 ]; then
    echo "Compiled files are missing or out-of-date. Running build.sh first..."
    "$PROJECT_DIR/build.sh"
fi

# 2. Locate java binary
JAVA_CMD=""
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVA_CMD="$(command -v java)"
else
    CANDIDATES=(
        "/var/lib/flatpak/app/org.apache.netbeans/x86_64/stable/active/files/jdk/bin/java"
        /usr/lib64/jvm/*/bin/java
        /usr/lib/jvm/*/bin/java
    )
    for c in "${CANDIDATES[@]}"; do
        if [ -x "$c" ]; then
            JAVA_CMD="$c"
            break
        fi
    done
fi

if [ -z "$JAVA_CMD" ]; then
    echo "Error: java could not be found." >&2
    echo "Please install Java (JRE/JDK) or set JAVA_HOME." >&2
    exit 1
fi

# 3. Locate JavaFX modules
JFX_VERSION="21.0.5"
M2_JFX="$HOME/.m2/repository/org/openjfx"
JFX_MODULES=("javafx-base" "javafx-graphics" "javafx-controls" "javafx-media" "javafx-web")
JFX_PATH=""

for mod in "${JFX_MODULES[@]}"; do
    JAR_PATH="$M2_JFX/$mod/$JFX_VERSION/$mod-$JFX_VERSION-linux.jar"
    if [ ! -f "$JAR_PATH" ]; then
        JAR_PATH="$(find "$M2_JFX/$mod" -name "$mod-*-linux.jar" 2>/dev/null | sort -V | tail -n 1)"
    fi
    if [ -f "$JAR_PATH" ]; then
        if [ -z "$JFX_PATH" ]; then
            JFX_PATH="$JAR_PATH"
        else
            JFX_PATH="$JFX_PATH:$JAR_PATH"
        fi
    fi
done

if [ -z "$JFX_PATH" ]; then
    echo "Error: JavaFX jars not found in $M2_JFX." >&2
    exit 1
fi

CACHE_DIR="/tmp/openjfx-cache"
mkdir -p "$CACHE_DIR" 2>/dev/null || true

if [ -z "$DISPLAY" ] && [ -z "$WAYLAND_DISPLAY" ]; then
    echo "Warning: Neither DISPLAY nor WAYLAND_DISPLAY is set. GUI applications require a desktop session." >&2
fi

echo "Launching Calculator..."
exec "$JAVA_CMD" \
    -Djavafx.cachedir="$CACHE_DIR" \
    --module-path "$JFX_PATH" \
    --add-modules javafx.controls,javafx.media,javafx.web \
    --enable-native-access=javafx.graphics \
    -cp "$PROJECT_DIR/target/classes" \
    com.calculator.app.Calculator "$@"
