#!/bin/bash
set -e

name=$(dpkg-parsechangelog --show-field=Source)

echo "Building ${name} in $(pwd)..."
cp -v ../orig/${name}_* ../
export LANG=C
export DEBIAN_FRONTEND=noninteractive
apt-get update
echo "Installing build dependencies..."
cd ..
mk-build-deps --install --remove \
	--tool \
	'apt-get -yqq --no-install-recommends -o Dpkg::Options::=--force-unsafe-io' \
	source/debian/control
cd source

echo "Building source package..."
debuild -S -sa -Zxz --source-option=--tar-ignore

echo "Copying result..."
cp -v ../${name}_*.{dsc,build,buildinfo,changes,tar.?z} . || true
