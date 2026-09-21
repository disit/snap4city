#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
platform="${DOCKER_PLATFORM:-linux/arm/v7}"
case "$platform" in
  linux/arm/v7) arch=armv7 ;;
  linux/arm64) arch=arm64 ;;
  *) echo "Usare linux/arm/v7 oppure linux/arm64" >&2; exit 1 ;;
esac
mkdir -p dist
docker buildx build --platform "$platform" --tag snap4city-nodered:2.2.2-arm \
  --output "type=docker,dest=dist/snap4city-nodered-2.2.2-$arch.tar" .
echo "Immagine creata: dist/snap4city-nodered-2.2.2-$arch.tar"
