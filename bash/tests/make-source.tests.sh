#! /bin/sh

BASEDIR=$(pwd)

setUp() {
	cd $BASEDIR
	rm -f test-package_*
	cd test-package
	git checkout -q debian/source/format
	git checkout -q debian/changelog
	rm -f debian/files
	cd $BASEDIR
}

tearDown() {
	cd $BASEDIR
	rm -f test-package_*
	cd test-package
	git checkout -q debian/source/format
	git checkout -q debian/changelog
	rm -f debian/files
}

testMakeSource_CanBuildInPlace() {
	cd test-package
	assertTrue "make-source failed" "../../make-source --build-in-place"
	cd ..
	assertTrue "dsc does not exist" "[ -f test-package_0.0.1-1.nightly*.dsc ]"
	assertTrue "orig.tar.xz does not exist" "[ -f test-package_0.0.1-1.nightly*.orig.tar.xz ]"
}

# Load shUnit2.
. shunit2