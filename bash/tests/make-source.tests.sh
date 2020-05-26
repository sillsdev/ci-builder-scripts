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
	cd test-package
	assertTrue "make-source failed" "../../make-source --build-in-place"
	cd ..
	assertTrue "dsc does not exist" "[ -f test-package_0.0.1-1.nightly*.dsc ]"
	assertTrue "orig.tar.xz does not exist" "[ -f test-package_0.0.1-1.nightly*.orig.tar.xz ]"
}

echo -e "\033[0;32mRunning tests in $0...\033[0m"

checkRequirements

# Load shUnit2.
. shunit2