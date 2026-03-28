#!/bin/bash
set -e

# Build the Go bridge as a shared library
cd "$(dirname "$0")/../go-bridge"
echo "Building libgovpn.so..."
go build -buildmode=c-shared -o ../desktop/libs/libgovpn.so bridge.go

echo "Building vpn-helper..."
cd helper
go build -o ../../desktop/libs/vpn-helper main.go

echo "Build successful: desktop/libs/libgovpn.so and desktop/libs/vpn-helper"
