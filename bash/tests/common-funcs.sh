cleanup() {
	cd $BASEDIR
	rm -f test-package_*
	rm -rf results/
	cd test-package
	git checkout -q debian/source/format
	git checkout -q debian/changelog
	rm -f debian/files
	cd $BASEDIR
}

checkRequirements() {
	if ! schroot -l | grep -q chroot:focal-amd64 ; then
		echo -e "\033[0;31mThese checks require a focal-amd64 chroot. Ignoring tests.\033[0m"
		exit
	fi
}