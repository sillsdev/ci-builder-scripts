RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

cleanup() {
	cd $BASEDIR
	rm -f test-package_*
	rm -rf results/
	cd test-package
	git clean -q -dxf
	git checkout -q debian/source/format
	git checkout -q debian/changelog
	rm -f debian/files
	cd $BASEDIR
}
