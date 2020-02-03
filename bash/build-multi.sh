#!/bin/bash

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
			if [ ! -e $CHANGES ]; then
				CHANGES="$RESULT/${PACKAGE}+${DIST}1_${ARCH}.changes"
			fi

			OPTS=()

			if [ $ARCH = amd64 ]; then
				if [ -n "$NO_SOURCE_PACKAGE" ]; then
					OPTS+=(--debbuildopt=-b)
				fi
			else
				# Don't build arch-independent packages - that's done with amd64
				OPTS+=(--no-arch-any)

				# Don't build if not for this arch
				if ! has_arch $ARCH $SRC
				then
					log "not for $ARCH so not building for it"
					continue
				fi
			fi

			if [ $SRC -nt $CHANGES ]; then
				log "PACKAGE=$PACKAGE DIST=$DIST ARCH=$ARCH"
				$NOOP setarch $(cpuarch $ARCH) sbuild --dist=$DIST --arch=$ARCH \
					--make-binNMU="Build for $DIST" -m "Package Builder <jenkins@sil.org>" \
					--append-to-version=+${DIST}1 --binNMU=0 "${OPTS[@]}" $SRC
				log "Done building: PACKAGE=$PACKAGE DIST=$DIST ARCH=$ARCH"
				echo $? | $NOOP tee $RESULT/${PACKAGE}_$ARCH.status
				$NOOP cp ${PACKAGE}*${DIST}*${ARCH} $RESULT

				if [ -n "$NO_SOURCE_PACKAGE" ]; then
					$NOOP rm -f $RESULT/${PACKAGE}.{dsc,{debian.,orig.,}tar.*}
				fi
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
