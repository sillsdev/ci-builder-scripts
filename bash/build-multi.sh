#!/bin/bash

PROGRAM_NAME="$(basename "$0")"

. $(dirname $0)/common.sh
general_init

set -e

NOOP=

if [ "$1" = "-n" ]; then
	NOOP=:
	shift
elif [ "$1" = "-nn" ]; then
	NOOP=echo
	shift
elif [ "$1" = "-nnn" ]; then
	NOOP=TRACE
	shift
fi

cpuarch()
{
	case $1 in
	amd64) echo x86_64;;
	*)     echo $1;;
	esac
}

get_field() # FIELD SRC
{
	if grep -q "BEGIN PGP SIGNED MESSAGE" $2
	then
		DECRYPT="gpg -d"
	else
		DECRYPT="cat"
	fi

	$DECRYPT $2 2>/dev/null | grep-dctrl -s$1 -n :
}

has_arch() # ARCH SRC
{
	local A

	for A in $(get_field Architecture $2)
	do
		if [ $A = $1 -o $A = any -o linux-any ]
		then
			return 0
		fi
	done
	return 1
}

binaries()
{
	get_field Binary $1 | sed 's/,//g'
}

checkAndInstallRequirements()
{
	local TOINSTALL
	if [ ! -x /usr/bin/unbuffer ]; then
		TOINSTALL="$TOINSTALL expect"
	fi

	if [ -n "$TOINSTALL" ]; then
		log "Installing prerequisites: $TOINSTALL"
		sudo apt-get update
		sudo apt-get -qy install $TOINSTALL
	fi
}

if [ "$1" = "--no-source-package" -o "$1" = "-n" ]; then
	NO_SOURCE_PACKAGE=true
	log "Not saving distribution-specific source package."
	shift
fi

if [[ "$1" != *.dsc ]]; then
	log "Usage: $0 [-n|--no-source-package] <package>.dsc [<package2>.dsc ...]"
	log "Options:"
	log "-n|--no-source-package\tdon't create distribution-specific source package"
	exit 1
fi

checkAndInstallRequirements

export DIST ARCH

for SRC
do
	PACKAGE=$(basename "$SRC" .dsc)

	for DIST in $DISTRIBUTIONS
	do
		for ARCH in $ARCHES
		do
			RESULT="$RESULTBASE/$DIST-$ARCH/result"
			$NOOP mkdir -p $RESULT
			log "Processing ${DIST}-${ARCH}"

			if [ ! -d "$RESULT" ]
			then
				log "Directory $RESULT doesn't exist - skipping"
				continue
			fi

			CHANGES="$RESULT/${PACKAGE}_${ARCH}.changes"
			if [ ! -e "$CHANGES" ]; then
				CHANGES="$RESULT/${PACKAGE}+${DIST}1_${ARCH}.changes"
			fi

			OPTS=()

			if [ $ARCH = amd64 ]; then
				OPTS+=(--arch-all)
				if [ -n "$NO_SOURCE_PACKAGE" ]; then
					OPTS+=(--debbuildopt=-b)
				fi
			else
				# Don't build arch-independent packages - that's done with amd64
				OPTS+=(--no-arch-all)

				# Don't build if not for this arch
				if ! has_arch $ARCH $SRC
				then
					log "not for $ARCH so not building for it"
					continue
				fi
			fi

			if [ $SRC -nt $CHANGES ]; then
				# Set the build profile. This allows to conditionally include build dependencies
				# in the `control` file, e.g.
				# Build-Depends: python3-qrcode <!pkg.keyman-config.xenial>
				# See https://wiki.debian.org/BuildProfileSpec
				export DEB_BUILD_PROFILES=pkg.${PACKAGE%_*}.${DIST}

				# older Ubuntu versions (xenial) don't support extension profile names, therefore
				# we skip that lintian check
				if [ "$DIST" == "xenial" ]; then
					OPTS+=(--lintian-opt="--suppress-tags=invalid-profile-name-in-source-relation")
				fi

				# We have to use the `aptitude` build-dep-resolver - the default `apt` one
				# always takes the first dependency of alternative build dependencies.
				log "PACKAGE=$PACKAGE DIST=$DIST ARCH=$ARCH"
				if [ "$(lsb_release -c -s)" == "xenial" ]; then
					$NOOP setarch $(cpuarch $ARCH) unbuffer sbuild --dist=$DIST --arch=$ARCH \
						--make-binNMU="Build for $DIST" -m "Package Builder <jenkins@sil.org>" \
						--append-to-version=+${DIST}1 --binNMU=0 --build-dep-resolver=aptitude \
						--arch-any "${OPTS[@]}" --purge=always $SRC
					$NOOP echo $? > $RESULT/${PACKAGE}_$ARCH.status
				else
					PKGNAME=${PACKAGE%%_*}
					PKGVERSION=${PACKAGE##*_}
					set -f # don't expand wildcards
					CHANGELOG=$(mktemp)
					echo "${PKGNAME} (${PKGVERSION}+${DIST}1) ${DIST}; urgency=medium" > $CHANGELOG
					echo "" >> $CHANGELOG
					echo "  * Build for ${DIST}" >> $CHANGELOG
					echo "" >> $CHANGELOG
					echo " -- Package Builder <jenkins@sil.org>  $(date -R)" >> $CHANGELOG
					$NOOP setarch $(cpuarch $ARCH) unbuffer sbuild --dist=$DIST --arch=$ARCH \
						--binNMU-changelog="$(cat ${CHANGELOG})" --build-dep-resolver=aptitude \
						--arch-any "${OPTS[@]}" --purge=always $SRC
					$NOOP echo $? > $RESULT/${PACKAGE}_$ARCH.status
					rm $CHANGELOG
					set +f
				fi

				echo "Exit code from sbuild: $(cat $RESULT/${PACKAGE}_$ARCH.status)"

				log "Copying files to $RESULT"
				if grep -q '.buildinfo$' ${PACKAGE}+${DIST}1_${ARCH}.changes ; then
					# Starting with Bionic the package contains a .buildinfo file that's required
					# to upload the package
					DCMD_ARGS="--deb --changes --buildinfo"
					DCMD_NOARGS="--no-deb --no-changes --no-orig --no-debtar --no-dsc --no-buildinfo"
				else
					DCMD_ARGS="--deb --changes"
					DCMD_NOARGS="--no-deb --no-changes --no-orig --no-debtar --no-dsc"
				fi
				$NOOP dcmd $DCMD_ARGS cp ${PACKAGE}+${DIST}1_${ARCH}.changes $RESULT/
				if grep -q '.ddeb$' ${PACKAGE}+${DIST}1_${ARCH}.changes ; then
					$NOOP dcmd $DCMD_NOARGS cp ${PACKAGE}+${DIST}1_${ARCH}.changes $RESULT/
				fi
				if [ -n "$NO_SOURCE_PACKAGE" ]; then
					$NOOP rm -f $RESULT/${PACKAGE}.{dsc,{debian.,orig.,}tar.*}
				else
					if [ $( ls ${PACKAGE}.dsc 2> /dev/null | wc -l) -ge 1 ]; then
						$NOOP dcmd --dsc --orig --debtar cp ${PACKAGE}.dsc $RESULT/
					fi
				fi
				log "Done building: PACKAGE=$PACKAGE DIST=$DIST ARCH=$ARCH"
			else
				if [ -e $CHANGES ]; then
					err "Not building $PACKAGE for $DIST/$ARCH because it already exists"
				elif [ ! -e $SRC ]; then
					err "Not building $PACKAGE for $DIST/$ARCH - \"$SRC\" doesn't exist"
				else
					err "Not building $PACKAGE for $DIST/$ARCH - unknown reason (SRC=$SRC, CHANGES=$CHANGES)"
				fi
			fi
		done
	done
done
