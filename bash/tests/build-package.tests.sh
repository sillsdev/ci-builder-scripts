#! /bin/bash

BASEDIR=$(pwd)

. common-funcs.sh

setUp() {
	cleanup
	export PACKAGING_ROOT=$(mktemp -d)
	unset WORKSPACE
	unset BUILD_NUMBER

	cd test-package
	../../make-source --build-in-place > /dev/null 2>&1
	cd ..
}

tearDown() {
	cleanup
	rm -rf $PACKAGING_ROOT
}

testBuildPackage_CanBuildInPlace() {
	# This scenario is used when building locally

	# Execute
	assertTrue "build-package failed" "../build-package --dists focal --arches amd64 --build-in-place --no-upload"

	# Verify
	assertTrue ".dsc does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1.dsc ]"
	assertTrue ".deb does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1+focal1_amd64.deb ]"
	assertTrue ".changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1+focal1_amd64.changes ]"
	assertTrue ".buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1+focal1_amd64.buildinfo ]"
	assertTrue "_source.buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1_source.buildinfo ]"
	assertTrue "_source.changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1_source.changes ]"
	assertTrue ".orig.tar.xz does not exist" "[ -f results/test-package_0.0.1-1.nightly*.orig.tar.xz ]"
	assertTrue ".debian.tar.xz does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1.debian.tar.xz ]"
	assertTrue "wrong checksum for a file in *_source.changes" "dscverify --noconf --nosigcheck results/test-package*_source.changes"
	assertTrue "wrong checksum for a file in *_amd64.changes" "dscverify --noconf --nosigcheck results/test-package*_amd64.changes"
	assertTrue "wrong checksum for a file in *.dsc" "dscverify --noconf --nosigcheck results/test-package*.dsc"
	assertTrue "wrong checksum for a file in *_source.buildinfo" "dscverify --noconf --nosigcheck results/test-package*_source.buildinfo"
	assertTrue "wrong checksum for a file in *_amd64.buildinfo" "dscverify --noconf --nosigcheck results/test-package*_amd64.buildinfo"
}

testBuildPackage_CanBuildInPlaceWithMainPackageName() {
	# This scenario is used for most package builds on Jenkins

	# Setup
	export WORKSPACE=$PACKAGING_ROOT/workspace
	mkdir -p $WORKSPACE
	mv test-package_0.0.1* $WORKSPACE
	cd test-package

	# Execute
	assertTrue "build-package failed" "../../build-package --dists focal --arches amd64 --no-upload --main-package-name test-package"

	# Verify
	assertTrue ".dsc does not exist" "[ -f $WORKSPACE/results/test-package_0.0.1-1.nightly*-1.dsc ]"
	assertTrue ".deb does not exist" "[ -f $WORKSPACE/results/test-package_0.0.1-1.nightly*-1+focal1_amd64.deb ]"
	assertTrue ".changes does not exist" "[ -f $WORKSPACE/results/test-package_0.0.1-1.nightly*-1+focal1_amd64.changes ]"
	assertTrue ".buildinfo does not exist" "[ -f $WORKSPACE/results/test-package_0.0.1-1.nightly*-1+focal1_amd64.buildinfo ]"
	assertTrue "_source.buildinfo does not exist" "[ -f $WORKSPACE/results/test-package_0.0.1-1.nightly*-1_source.buildinfo ]"
	assertTrue "_source.changes does not exist" "[ -f $WORKSPACE/results/test-package_0.0.1-1.nightly*-1_source.changes ]"
	assertTrue ".orig.tar.xz does not exist" "[ -f $WORKSPACE/results/test-package_0.0.1-1.nightly*.orig.tar.xz ]"
	assertTrue ".debian.tar.xz does not exist" "[ -f $WORKSPACE/results/test-package_0.0.1-1.nightly*-1.debian.tar.xz ]"
	assertTrue "wrong checksum for a file in *_source.changes" "dscverify --noconf --nosigcheck $WORKSPACE/results/test-package*_source.changes"
	assertTrue "wrong checksum for a file in *_amd64.changes" "dscverify --noconf --nosigcheck $WORKSPACE/results/test-package*_amd64.changes"
	assertTrue "wrong checksum for a file in *.dsc" "dscverify --noconf --nosigcheck $WORKSPACE/results/test-package*.dsc"
	assertTrue "wrong checksum for a file in *_source.buildinfo" "dscverify --noconf --nosigcheck $WORKSPACE/results/test-package*_source.buildinfo"
	assertTrue "wrong checksum for a file in *_amd64.buildinfo" "dscverify --noconf --nosigcheck $WORKSPACE/results/test-package*_amd64.buildinfo"
}

testBuildPackage_WorksWithDscInCurrentDir() {
	# This scenario is used when building Keyman packages

	# Setup
	mv test-package_0.0.1* test-package/
	cd test-package

	# Execute
	assertTrue "build-package failed" "../../build-package --dists focal --arches amd64 --build-in-place --no-upload"

	# Verify
	assertTrue ".dsc does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1.dsc ]"
	assertTrue ".deb does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1+focal1_amd64.deb ]"
	assertTrue ".changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1+focal1_amd64.changes ]"
	assertTrue ".buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1+focal1_amd64.buildinfo ]"
	assertTrue "_source.buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1_source.buildinfo ]"
	assertTrue "_source.changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1_source.changes ]"
	assertTrue ".orig.tar.xz does not exist" "[ -f results/test-package_0.0.1-1.nightly*.orig.tar.xz ]"
	assertTrue ".debian.tar.xz does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1.debian.tar.xz ]"
	assertTrue "wrong checksum for a file in *_source.changes" "dscverify --noconf --nosigcheck results/test-package*_source.changes"
	assertTrue "wrong checksum for a file in *_amd64.changes" "dscverify --noconf --nosigcheck results/test-package*_amd64.changes"
	assertTrue "wrong checksum for a file in *.dsc" "dscverify --noconf --nosigcheck results/test-package*.dsc"
	assertTrue "wrong checksum for a file in *_source.buildinfo" "dscverify --noconf --nosigcheck results/test-package*_source.buildinfo"
	assertTrue "wrong checksum for a file in *_amd64.buildinfo" "dscverify --noconf --nosigcheck results/test-package*_amd64.buildinfo"
}

testBuildPackage_WorksForPrs() {
	# This scenario is used when building Keyman packages for PRs

	# Setup
	mv test-package_0.0.1* test-package/
	cd test-package

	export BUILD_NUMBER=987

	# Execute
	assertTrue "build-package failed" "../../build-package --dists focal --arches amd64 --build-in-place --no-upload --prerelease-tag ~PR-1234"

	# Verify
	assertTrue ".dsc does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1.dsc ]"
	assertTrue ".deb does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1~PR-1234+focal987_amd64.deb ]"
	assertTrue ".changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1~PR-1234+focal987_amd64.changes ]"
	assertTrue ".buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1~PR-1234+focal987_amd64.buildinfo ]"
	assertTrue "_source.buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1_source.buildinfo ]"
	assertTrue "_source.changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1_source.changes ]"
	assertTrue ".orig.tar.xz does not exist" "[ -f results/test-package_0.0.1-1.nightly*.orig.tar.xz ]"
	assertTrue ".debian.tar.xz does not exist" "[ -f results/test-package_0.0.1-1.nightly*-1.debian.tar.xz ]"
	assertTrue "wrong checksum for a file in *_source.changes" "dscverify --noconf --nosigcheck results/test-package*_source.changes"
	assertTrue "wrong checksum for a file in *_amd64.changes" "dscverify --noconf --nosigcheck results/test-package*_amd64.changes"
	assertTrue "wrong checksum for a file in *.dsc" "dscverify --noconf --nosigcheck results/test-package*.dsc"
	assertTrue "wrong checksum for a file in *_source.buildinfo" "dscverify --noconf --nosigcheck results/test-package*_source.buildinfo"
	assertTrue "wrong checksum for a file in *_amd64.buildinfo" "dscverify --noconf --nosigcheck results/test-package*_amd64.buildinfo"
}

echo -e "${GREEN}Running tests in $0...${NC}"

if ! schroot -l | grep -q chroot:focal-amd64 ; then
	echo -e "${RED}These checks require a focal-amd64 chroot. Ignoring tests.${NC}"
	exit
fi

# Load shUnit2.
. ./shunit2
