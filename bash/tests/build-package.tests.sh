#! /bin/bash

BASEDIR=$(pwd)

. common-funcs.sh

setUp() {
	cleanup

	cd test-package
	../../make-source --build-in-place > /dev/null 2>&1
	cd ..
	unset WORKSPACE
}

tearDown() {
	cleanup
}

testBuildPackage_CanBuildInPlace() {
	# This scenario is used when building locally

	# Execute
	assertTrue "build-package failed" "../build-package --dists focal --arches amd64 --build-in-place --no-upload"

	# Verify
	assertTrue ".deb does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.deb ]"
	assertTrue ".changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.changes ]"
	assertTrue ".buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.buildinfo ]"
}

testBuildPackage_CanBuildInPlaceWithMainPackageName() {
	# This scenario is used for most package builds on Jenkins

	# Execute
	assertTrue "build-package failed" "../build-package --dists focal --arches amd64 --build-in-place --no-upload --main-package-name test-package"

	# Verify
	assertTrue ".deb does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.deb ]"
	assertTrue ".changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.changes ]"
	assertTrue ".buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.buildinfo ]"
}

testBuildPackage_WorksWithDscInCurrentDir() {
	# This scenario is used when building Keyman packages

	# Setup
	mv test-package_0.0.1* test-package/
	cd test-package

	# Execute
	assertTrue "build-package failed" "../../build-package --dists focal --arches amd64 --build-in-place --no-upload"

	# Verify
	assertTrue ".deb does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.deb ]"
	assertTrue ".changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.changes ]"
	assertTrue ".buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.buildinfo ]"
}

echo -e "${GREEN}Running tests in $0...${NC}"

if ! schroot -l | grep -q chroot:focal-amd64 ; then
	echo -e "${RED}These checks require a focal-amd64 chroot. Ignoring tests.${NC}"
	exit
fi

# Load shUnit2.
. ./shunit2
