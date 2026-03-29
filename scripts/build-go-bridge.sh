#!/bin/bash
set -e

# Ensure Go dependencies are present
PROJECT_ROOT="$(dirname "$0")/.."
cd "$PROJECT_ROOT"

ensure_repo() {
    local dir=$1
    local url=$2
    if [ ! -d "$dir" ]; then
        echo "Cloning $dir..."
        git clone "$url" "$dir"
    else
        echo "$dir already exists, skipping clone."
    fi
}

ensure_repo "go-vpn-lib" "https://github.com/ProtonVPN/go-vpn-lib"
ensure_repo "go-srp" "https://github.com/ProtonMail/go-srp"
ensure_repo "amneziawg-go" "https://github.com/amnezia-vpn/amneziawg-go"

# Build the Go bridge as a shared library
cd "go-bridge"
echo "Building libgovpn.so..."
go build -buildmode=c-shared -o ../desktop/libs/libgovpn.so bridge.go

echo "Building vpn-helper..."
cd helper
go build -o ../../desktop/libs/vpn-helper main.go

echo "Build successful: desktop/libs/libgovpn.so and desktop/libs/vpn-helper"
