#!/bin/bash

stderr()
{
	echo -e "${RED}$PROGRAM_NAME: $1${NC}" >&2
}

log()
{
	echo -e "${GREEN}$PROGRAM_NAME: $1${NC}" >&2
}

init()
{
	# Process arguments.
	while (( $# )); do
		case $1 in
			# Process individual arguments here. Use shift and $1 to get an argument value.
			# Example: -d) DEBUG=true ;;
			# Example: --outfile) shift; OUTFILE=$1 ;;
			# Example: *) echo "Unexpected argument: $1"; exit 1 ;;
			--debkeyid) shift; debkeyid=$1 ;;
			# Space-delimited list of releases. eg "precise raring"
			--dists) shift; dists_arg=$1 ;;
			# Space-delimited list of architectures. eg "amd64 i386"
			--arches) shift; arches_arg=$1 ;;
			# Comma-delimited list of non-default repositorydir=committish mapping (hash, tag, branch). eg "fwrepo/fw=2568e4f,fwrepo/fw/Localizations=linux/FieldWorks8.0.3-beta4,fwrepo/fw/DistFiles/Helps=origin/master"
			--repository-committishes) shift; repository_committishes_arg=$1 ;;
			# Don't upload packages at the end
			--simulate-dput) dput_simulate="-s" ;;
			--package-version-extension) shift; package_version_extension=$1 ;;
			# Suite location: eg. experimental, updates, proposed, dictionary
			--suite-name) shift; suite_name=$1 ;;
			--main-package-name) shift; main_package_name_arg=$1 ;;
			# Skip cleaning and updating local repository
			--preserve-repository) preserve_repository_arg=true ;;
			# Space-delimited list of binary packages to remove from debian/control file before creating source package
			--omit-binary-packages) shift; omit_binary_packages_arg=$1 ;;
			# For making release packages. Do not add a new entry to the changelog. Package versions will be as specified in the last changelog entry, without a nightly timestamp appended.
			--preserve-changelog) preserve_changelog_arg=true ;;
			# Omit uploading to llso. This parameter should be set when doing a release build on
			# Jenkins because the package has to be downloaded, signed and manually uploaded.
			--no-upload) no_upload=true ;;
			# The distros we might possibly want to build
			--supported-distros) shift; supported_distros_arg=$1 ;;
			# The package version to use instead a version number based on the last version
			# from the changelog. Any 0 will be replaced by the corresponding number from the
			# changelog, e.g. passing 0.0.123.456 with a version from changelog of 3.1.2.3
			# will result in 3.1.123.456.
			--package-version) shift; package_version=$1 ;;
			# The subdirectory of the main repo, e.g. fw. Default is the current directory.
			--main-repo-dir) shift; main_repo_dir=$1 ;;
			# The name of the directory where the source code resides. Default is the name of the source package.
			# This directory is relative to $repo_base_dir.
			--source-code-subdir) shift; source_package_dir=$1 ;;
			--no-package) no_package=true ;;
			# append the argument to the package name. Only relevant for make-source.
			--append-to-package) shift; append_to_package=$1;;
			# use current directory to build source package instead of $repo_base_dir/${$source_package_name}
			--build-in-place) build_in_place=true ;;
			# use argument as delimiter that seperates the version number from the 'nightly' string
			--nightly-delimiter) shift; nightlydelimeter=$1;;
			*) stderr "Error: Unexpected argument \"$1\". Exiting." ; exit 1 ;;
		esac
		shift || (stderr "Error: The last argument is missing a value. Exiting."; false) || exit 2
	done

	DISTRIBUTIONS_TO_PACKAGE="${dists_arg:-trusty}"
	DISTS_TO_PROCESS="${supported_distros_arg:-trusty xenial}"
	ARCHES_TO_PACKAGE="${arches_arg:-i386 amd64}"
	ARCHES_TO_PROCESS="amd64 i386"
	PACKAGING_ROOT="$HOME/packages"
	SUITE_NAME="${suite_name:-experimental}"
	[ -z $PBUILDER_TOOLS_PATH ] && PBUILDER_TOOLS_PATH="$HOME/FwSupportTools/packaging/pbuilder"

	pbuilder_path="${PBUILDERDIR:-$HOME/pbuilder}"

	if [ -z "$no_package" ]; then
		# set Debian/changelog environment
		export DEBFULLNAME="${main_package_name_arg:-Unknown} Package Signing Key"
		export DEBEMAIL='jenkins@sil.org'

		repo_base_dir=${WORKSPACE:-$PACKAGING_ROOT/$main_package_name_arg}
		debian_path="debian"
		source_package_name=$(dpkg-parsechangelog |grep ^Source:|cut -d' ' -f2)
	fi

	if [ -d ".hg" ]; then
		VCS=hg
	else
		VCS=git
	fi

	RED='\033[0;31m'
	GREEN='\033[0;32m'
	NC='\033[0m' # No Color
}
