#! /bin/bash

BASEDIR=$(pwd)

. common-funcs.sh

setUp() {
	cleanup
}

tearDown() {
	cleanup
}

testMakeSource_CanBuildInPlace() {
	# Execute
	cd test-package
	assertTrue "make-source failed" "../../make-source --build-in-place"

	# Verify
	cd ..
	assertTrue ".dsc does not exist" "[ -f test-package_0.0.1-1.nightly*.dsc ]"
	assertTrue "_source.changes does not exist" "[ -f test-package_0.0.1-1.nightly*_source.changes ]"
	assertTrue ".orig.tar.xz does not exist" "[ -f test-package_0.0.1-1.nightly*.orig.tar.xz ]"
	assertTrue ".debian.tar.xz does not exist" "[ -f test-package_0.0.1-1.nightly*.debian.tar.xz ]"
}

echo -e "${GREEN}Running tests in $0...${NC}"

# Load shUnit2.
. ./shunit2
