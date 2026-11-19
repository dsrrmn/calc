#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=== Building Calculator & Mania ==="

# 1. Locate javac
JAVAC=""
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
elif command -v javac >/dev/null 2>&1; then
    JAVAC="$(command -v javac)"
else
    CANDIDATES=(
        "/var/lib/flatpak/app/org.apache.netbeans/x86_64/stable/active/files/jdk/bin/javac"
        /usr/lib64/jvm/*/bin/javac
        /usr/lib/jvm/*/bin/javac
    )
    for c in "${CANDIDATES[@]}"; do
        if [ -x "$c" ]; then
            JAVAC="$c"
            break
        fi
    done
fi

if [ -z "$JAVAC" ]; then
    echo "Error: javac could not be found." >&2
    echo "Please install a JDK (e.g. 'sudo zypper install java-25-openjdk-devel') or set JAVA_HOME." >&2
    exit 1
fi

echo "Using javac: $JAVAC"

# 2. Locate JavaFX modules
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

# 3. Create output directory
mkdir -p "$PROJECT_DIR/target/classes"

# 4. Find all Java source files
SOURCES=$(find "$PROJECT_DIR/src/main/java" -name "*.java")

# 5. Compile
echo "Compiling Java sources..."
"$JAVAC" \
    --release 17 \
    --module-path "$JFX_PATH" \
    --add-modules javafx.controls,javafx.media,javafx.web \
    -d "$PROJECT_DIR/target/classes" \
    $SOURCES

# 6. Locate jar tool for packaging
JAR_TOOL=""
if [ -n "$JAVAC" ]; then
    CANDIDATE_JAR="$(dirname "$JAVAC")/jar"
    if [ -x "$CANDIDATE_JAR" ]; then
        JAR_TOOL="$CANDIDATE_JAR"
    fi
fi
if [ -z "$JAR_TOOL" ] && command -v jar >/dev/null 2>&1; then
    JAR_TOOL="$(command -v jar)"
fi

if [ -n "$JAR_TOOL" ]; then
    echo "Packaging target/calculator.jar..."
    "$JAR_TOOL" --create --file "$PROJECT_DIR/target/calculator.jar" \
        --main-class com.calculator.app.Launcher \
        -C "$PROJECT_DIR/target/classes" .
fi

echo "Build completed successfully!"
