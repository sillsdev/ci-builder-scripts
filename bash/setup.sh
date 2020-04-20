#!/bin/bash

# setup.sh
# Setup or update mirrors

set -e -o pipefail

PROGRAM=$(readlink -f "$0")
PROGRAM_NAME="$(basename "$0")"

. $(dirname "$0")/common.sh
no_package=true
init "$@"

function checkOrLinkDebootstrapScript()
{
	if [ ! -f /usr/share/debootstrap/scripts/$1 ]; then
		if [[ "$UBUNTU_DISTROS $UBUNTU_OLDDISTROS" == "*$1*" ]]; then
			basedistro=gutsy
		else
			basedistro=sid
		fi
		sudo ln -s /usr/share/debootstrap/scripts/$basedistro /usr/share/debootstrap/scripts/$1
	fi
}

function addmirror()
{
	OTHERMIRROR="$OTHERMIRROR${OTHERMIRROR:+|}$1"
}

# Create a source.list template file that will be used in the chroot
function createSources()
{
	cat > $1 <<EOF
$(echo $OTHERMIRROR | tr '|' '\n')
EOF
}

function enableBackports()
{
	cat > $1 <<EOF
Package: *
Pin: release a=$2-backports
Pin-Priority: 500
EOF
}

function checkAndInstallRequirements()
{
	local TOINSTALL GROUP
	if [ ! -x /usr/bin/mk-sbuild ]; then
		TOINSTALL="$TOINSTALL ubuntu-dev-tools"
	fi
	if [ ! -x /usr/bin/sbuild ]; then
		TOINSTALL="$TOINSTALL sbuild"
		touch $HOME/.sbuildrc
	fi

	if [ -n "$TOINSTALL" ]; then
		log "Installing prerequisites: $TOINSTALL"
		sudo apt-get update
		sudo apt-get -qy install $TOINSTALL
	fi

	GROUP=$(groups | grep sbuild || true)
	if [ -z "$GROUP" ]; then
		sudo adduser $USER sbuild > /dev/null || true
		local TEMPFILE=$(mktemp)
		local ARGS=()
		while (( $# )); do
			ARGS+=("\"$1\"")
			shift
		done
		echo "${ARGS[@]}" > $TEMPFILE
		chmod +x $TEMPFILE

		log "Calling $(basename ${ARGS[0]}) recursively"
		sg sbuild $TEMPFILE
		rm $TEMPFILE
		exit
	fi

	# We have to install a current version of mk-sbuild because trying to build newer dists
	# on an older dist might have different requirements than the system provided version
	# of mk-sbuild provides (e.g. on xenial when trying to build a bionic chroot).
	if [ ! -f $HOME/bin/mk-sbuild -o ! -f $HOME/bin/mk-sbuild.v${MKSBUILD_VERSION} ]; then
		log "Getting version ${MKSBUILD_VERSION} of mk-sbuild"
		mkdir -p $HOME/bin
		rm -f $HOME/bin/mk-sbuild*
		TRACE wget --output-document=$HOME/bin/mk-sbuild https://git.launchpad.net/ubuntu-dev-tools/plain/mk-sbuild?h=${MKSBUILD_VERSION}
		chmod +x $HOME/bin/mk-sbuild
		touch $HOME/bin/mk-sbuild.v${MKSBUILD_VERSION}
	fi
}

function copyInKeyrings()
{
	[ -f "${KEYRINGLLSO}" ] && sudo cp "${KEYRINGLLSO}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRINGPSO}" ] && sudo cp "${KEYRINGPSO}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRINGNODE}" ] && sudo cp "${KEYRINGNODE}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRINGMICROSOFT}" ] && sudo cp "${KEYRINGMICROSOFT}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRING_MONO}" ] && sudo cp "${KEYRING_MONO}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
}

function addExtraRepositories()
{
	TMPFILE="$(mktemp)"
	createSources "${TMPFILE}"
	log "Content of extra.list: $(cat $TMPFILE)"
	sudo cp "${TMPFILE}" "${SCHROOTDIR}/${D}-${A}/etc/apt/sources.list.d/extra.list"
	rm "${TMPFILE}"
}

WORKDIR="${WORKSPACE:-$(realpath $(dirname "$0"))}"

cd "${WORKDIR}"

checkAndInstallRequirements $PROGRAM "$@"

KEYRINGLLSO="$WORKDIR/llso-keyring-2013.gpg"
KEYRINGPSO="$WORKDIR/pso-keyring-2016.gpg"
KEYRINGNODE="$WORKDIR/nodesource-keyring.gpg"
KEYRINGMICROSOFT="$WORKDIR/microsoft.asc.gpg"
KEYRING_MONO="$WORKDIR/mono-project.asc.gpg"

if [ ! -f ${KEYRINGPSO} ]; then
	wget --output-document=${KEYRINGPSO} https://packages.sil.org/keys/pso-keyring-2016.gpg
fi

if [ ! -f ${KEYRINGLLSO} ]; then
	wget --output-document=${KEYRINGLLSO} http://linux.lsdev.sil.org/keys/llso-keyring-2013.gpg
fi

if [ ! -f ${KEYRINGNODE} ]; then
	# Download node key and convert to keyring.

	NODE_KEY="$(mktemp -d)/nodesource-key.asc"
	# Use a temporary, intermediate keyring since it may be keybox format on Ubuntu 20.04, and we need it to be an older format for apt.
	TMP_KEYRING=$(mktemp)
	wget --output-document=${NODE_KEY} https://deb.nodesource.com/gpgkey/nodesource.gpg.key
	gpg --trust-model always --no-default-keyring --keyring ${TMP_KEYRING} \
		--import ${NODE_KEY}
	# Export without --armor since gpg seems to have trouble inspecting an
	# armor export keyring.
	gpg --no-default-keyring --keyring ${TMP_KEYRING} --export > ${KEYRINGNODE}

	rm -f "${TMP_KEYRING}"
	rm -rf "$(dirname "${NODE_KEY}")"
fi

if [ ! -f ${KEYRINGMICROSOFT} ]; then
	wget -qO- https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor -o ${KEYRINGMICROSOFT}
fi

if [ ! -f ${KEYRING_MONO} ]; then
	TMP_KEYRING="$(mktemp)"
	XAMARIN_KEY_FINGERPRINT="3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF"
	gpg --trust-model always --no-default-keyring --keyring "${TMP_KEYRING}" \
		--keyserver keyserver.ubuntu.com --recv-keys ${XAMARIN_KEY_FINGERPRINT}
	gpg --no-default-keyring --keyring "${TMP_KEYRING}" --export > "${KEYRING_MONO}"
	rm -f "${TMP_KEYRING}"
fi


for D in ${dists_arg:-$UBUNTU_DISTROS $UBUNTU_OLDDISTROS $DEBIAN_DISTROS}
do
	for A in ${arches_arg:-amd64 i386}
	do
		[ -e $SCHROOTDIR/$D-$A -a -z "$update" ] && echo "$D-$A already exists - skipping creation" && continue
		[ ! -e $SCHROOTDIR/$D-$A -a -n "$update" ] && echo "$D-$A doesn't exist - skipping update" && continue

		log "Processing $D-$A"

		OTHERMIRROR=""

		checkOrLinkDebootstrapScript $D

		# packages.microsoft is a 64-bit only repo. 32-bit can be downloaded as a tar.
		MICROSOFT_APT="deb [arch=amd64] https://packages.microsoft.com/repos/microsoft-ubuntu-${D}-prod ${D} main"
		MONO_APT="deb https://download.mono-project.com/repo/ubuntu vs-${D} main"

		if [[ "$UBUNTU_DISTROS $UBUNTU_OLDDISTROS" == *$D* ]]; then
			if [[ $UBUNTU_DISTROS == *$D* ]]; then
				MIRROR="${UBUNTU_MIRROR:-http://archive.ubuntu.com/ubuntu/}"
			else
				MIRROR="${UBUNTU_OLDMIRROR:-http://old-releases.ubuntu.com/ubuntu/}"
			fi
			COMPONENTS="main universe multiverse"
			KEYRINGMAIN="/usr/share/keyrings/ubuntu-archive-keyring.gpg"
			PROXY="$http_proxy"
			for S in backports ; do
				addmirror "deb $MIRROR $D-$S $COMPONENTS"
			done
			LLSO="http://linux.lsdev.sil.org/ubuntu/"
			PSO="http://packages.sil.org/ubuntu/"
			for S in "" "-proposed" "-updates" "-experimental"; do
				addmirror "deb $LLSO $D$S $COMPONENTS"
				addmirror "deb $PSO $D$S $COMPONENTS"
			done
			# There's no node repo for focal yet.
			if [ $D != "precise" -a $D != "focal" ]; then
				# Allow to install current nodejs packages
				if [ -n "$update" ]; then
					# We can't use https when creating the chroot because apt-transport-https
					# isn't available yet. This is so for Ubuntu 16.04, but beginning in Ubuntu 18.04 the capability is probably built-in.
					# Adding apt-transport-https to pbuilder --debootstrapopts --include does not solve it.
					addmirror "deb https://deb.nodesource.com/node_8.x $D main"
					addmirror "${MICROSOFT_APT}"
					addmirror "${MONO_APT}"
				fi
			fi
		elif [[ $DEBIAN_DISTROS == *$D* ]]; then
			MIRROR="${DEBIAN_MIRROR:-http://ftp.ca.debian.org/debian/}"
			COMPONENTS="main contrib non-free"
			KEYRINGMAIN="/usr/share/keyrings/debian-archive-keyring.gpg"
			PROXY="$http_proxy"
			LLSO="http://linux.lsdev.sil.org/debian/"
			PSO="http://packages.sil.org/debian/"
			addmirror "deb $LLSO $D $COMPONENTS"
			addmirror "deb $PSO $D $COMPONENTS"
			if [ $D != "wheezy" ]; then
				# Allow to install current nodejs packages
				if [ -n "$update" ]; then
					# We can't use https when creating the chroot because apt-transport-https
					# isn't available yet. This is so for Debian stretch, but beginning in Debian buster the capability is probably built-in.
					addmirror "deb https://deb.nodesource.com/node_8.x $D main"
					addmirror "${MICROSOFT_APT}"
					addmirror "${MONO_APT}"
				fi
			fi
		else
			stderr "Unknown distribution $D. Please update the script $0."
			exit 1
		fi

		if [ -z "$update" ]; then
			# Create a new chroot
			log "Create new chroot for $D-$A"
			if [ "$D-$A" != "focal-i386" ]; then
				# eatmydata (or rather a package eatmydata depends on) is not available in
				# 32bit starting with focal
				OTHEROPTS=--eatmydata
			fi

			TRACE $HOME/bin/mk-sbuild $D --arch=$A \
				--debootstrap-include="perl,gnupg,debhelper" \
				${PROXY:+--debootstrap-proxy=}$PROXY \
				$OTHEROPTS --type=directory

			copyInKeyrings
			addExtraRepositories

			TMPFILE=$(mktemp)
			enableBackports $TMPFILE $D
			sudo cp $TMPFILE $SCHROOTDIR/$D-$A/etc/apt/preferences.d/backports
			rm $TMPFILE

			log "Install packages in chroot for $D-$A"
			#TRACE sudo schroot -c source:$D-$A -u root --directory=/ -- sh -c \
			#	"apt-get -qq update && \
			#	apt-get -qy upgrade && \
			#	apt-get -qy install apt-utils devscripts lsb-release apt-transport-https ca-certificates tzdata && \
			#	apt-get clean" < /dev/null
			TRACE sudo sbuild-update --update --dist-upgrade --upgrade $D-$A
			TRACE sudo sbuild-apt $D-$A apt-get install apt-utils devscripts lsb-release apt-transport-https ca-certificates tzdata
			TRACE sudo sbuild-update --clean --autoclean --autoremove $D-$A
		else
			# Update chroot
			log "Update chroot for $D-$A"
			copyInKeyrings
			addExtraRepositories
			#TRACE sudo schroot -c source:$D-$A -u root --directory=/ -- sh -c \
			#	"apt-get -qq update && apt-get -qy upgrade && apt-get clean" < /dev/null
			TRACE sudo sbuild-update --update --dist-upgrade --upgrade --clean --autoclean --autoremove $D-$A
		fi
	done
done
