#! /bin/bash

BASEDIR=$(pwd)

. common-funcs.sh

setUp() {
	cleanup
}

tearDown() {
	cleanup
}

testBuildPackage_CanBuildInPlace() {
	# Setup
	cd test-package
	../../make-source --build-in-place > /dev/null 2>&1

	# Execute
	assertTrue "build-package failed" "../../build-package --dists focal --arches amd64 --build-in-place --no-upload"

	# Verify
	cd ..
	assertTrue ".deb does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.deb ]"
	assertTrue ".changes does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.changes ]"
	assertTrue ".buildinfo does not exist" "[ -f results/test-package_0.0.1-1.nightly*+focal1_amd64.buildinfo ]"
}

echo -e "\033[0;32mRunning tests in $0...\033[0m"

checkRequirements

# Load shUnit2.
. shunit2