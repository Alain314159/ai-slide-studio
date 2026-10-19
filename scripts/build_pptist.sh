#!/usr/bin/env bash
# Builds PPTist (Vue 3 editor) and copies the compiled output into the
# Android app's assets directory so the WebView can load it offline.
#
# Usage:
#   ./scripts/build_pptist.sh
#
# Requirements:
#   - Node.js >= 18
#   - npm
#
# This script is called by the GitHub Action CI workflow before building
# the APK, and should also be run locally before opening in Android Studio
# if you want the editor to be available in the WebView.

set -euo pipefail

# Resolve script location
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PPTIST_DIR="$ROOT_DIR/editor/pptist"
ASSETS_DIR="$ROOT_DIR/app/src/main/assets/pptist"

echo "=== Building PPTist editor ==="
echo "Source: $PPTIST_DIR"
echo "Output: $ASSETS_DIR"
echo ""

# 1. Verify PPTist source exists
if [ ! -d "$PPTIST_DIR" ]; then
    echo "ERROR: PPTist source not found at $PPTIST_DIR"
    echo "Did you forget to clone it? Run:"
    echo "  git clone https://github.com/pipipi-pikachu/PPTist.git editor/pptist"
    exit 1
fi

# 2. Verify Node.js is available
if ! command -v node >/dev/null 2>&1; then
    echo "ERROR: Node.js is not installed. Install Node 18+ and retry."
    exit 1
fi
if ! command -v npm >/dev/null 2>&1; then
    echo "ERROR: npm is not installed. Install Node 18+ and retry."
    exit 1
fi

NODE_VERSION=$(node --version)
echo "Node version: $NODE_VERSION"
echo ""

# 3. Install dependencies (cached if package-lock.json unchanged)
cd "$PPTIST_DIR"
if [ ! -d node_modules ]; then
    echo "=== Installing npm dependencies (this may take a few minutes) ==="
    npm ci
else
    echo "=== node_modules already exists, skipping install ==="
fi
echo ""

# 4. Build PPTist
echo "=== Compiling PPTist ==="
npm run build-only
echo ""

# 5. Verify dist exists
if [ ! -d dist ]; then
    echo "ERROR: Build did not produce dist/ directory"
    exit 1
fi

# 6. Copy to Android assets
echo "=== Copying dist/ to Android assets ==="
mkdir -p "$ASSETS_DIR"
rm -rf "$ASSETS_DIR"/*
cp -r dist/* "$ASSETS_DIR/"

echo ""
echo "=== Build complete ==="
echo "Editor assets at: $ASSETS_DIR"
ls -la "$ASSETS_DIR" | head -20
