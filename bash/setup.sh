#!/bin/bash

# setup.sh
# Setup or update mirrors

set -e -o pipefail

. $(dirname $0)/common.sh
general_init

# Process arguments.
while (( $# )); do
	case $1 in
		# Process individual arguments here. Use shift and $1 to get an argument value.
		--update) update=true ;;
		*) stderr "Error: Unexpected argument \"$1\". Exiting." ; exit 1 ;;
	esac
	shift
done

function CheckOrLinkDebootstrapScript()
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

WORKDIR="${WORKSPACE:-$(realpath $(dirname "$0"))}"

cd "${WORKDIR}"

KEYRINGLLSO="$WORKDIR/llso-keyring-2013.gpg"
KEYRINGPSO="$WORKDIR/pso-keyring-2016.gpg"
KEYRINGNODE="$WORKDIR/nodesource-keyring.gpg"

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
	gpg --no-default-keyring --keyring ${TMP_KEYRING} --import ${NODE_KEY}
	# Export without --armor since gpg seems to have trouble inspecting an armor export keyring.
	gpg --no-default-keyring --keyring ${TMP_KEYRING} --export > ${KEYRINGNODE}

	rm -f $TMP_KEYRING
	rm -rf $(basedir $NODE_KEY)
fi

for D in ${DISTRIBUTIONS:-$UBUNTU_DISTROS $UBUNTU_OLDDISTROS $DEBIAN_DISTROS}
do
	for A in ${ARCHES-amd64 i386}
	do
		[ -e $SCHROOTDIR/$D-$A -a -z "$update" ] && echo "$D-$A already exists - skipping creation" && continue
		[ ! -e $SCHROOTDIR/$D-$A -a -n "$update" ] && echo "$D-$A doesn't exist - skipping update" && continue

		log "Processing $D-$A"

		OTHERMIRROR=""

		CheckOrLinkDebootstrapScript $D

		if [[ "$UBUNTU_DISTROS $UBUNTU_OLDDISTROS" == *$D* ]]; then
			if [[ $UBUNTU_DISTROS == *$D* ]]; then
				MIRROR="${UBUNTU_MIRROR:-http://archive.ubuntu.com/ubuntu/}"
			else
				MIRROR="${UBUNTU_OLDMIRROR:-http://old-releases.ubuntu.com/ubuntu/}"
			fi
			COMPONENTS="main universe multiverse"
			KEYRINGMAIN="/usr/share/keyrings/ubuntu-archive-keyring.gpg"
			PROXY="$http_proxy"
			for S in backports updates; do
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
				# allow to install current nodejs packages
				if [ -n "$update" ]; then
					# we can't use https when creating the chroot because apt-transport-https
					# isn't available yet
					addmirror "deb https://deb.nodesource.com/node_8.x $D main"
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
				# allow to install current nodejs packages
				if [ -n "$update" ]; then
					# we can't use https when creating the chroot because apt-transport-https
					# isn't available yet
					addmirror "deb https://deb.nodesource.com/node_8.x $D main"
				fi
			fi
		else
			stderr "Unknown distribution $D. Please update the script $0."
			exit 1
		fi

		if [ -z "$update" ]; then
			# Create a new chroot
			TRACE mk-sbuild $D --arch=$A \
				--debootstrap-include="perl,gnupg" \
				${PROXY:+--debootstrap-proxy=}$PROXY \
				--eatmydata --type=directory

			[ -f $KEYRINGLLSO ] && sudo cp $KEYRINGLLSO $SCHROOTDIR/$D-$A/etc/apt/trusted.gpg.d/
			[ -f $KEYRINGPSO ] && sudo cp $KEYRINGPSO $SCHROOTDIR/$D-$A/etc/apt/trusted.gpg.d/
			[ -f $KEYRINGNODE ] && sudo cp $KEYRINGNODE $SCHROOTDIR/$D-$A/etc/apt/trusted.gpg.d/

			TMPFILE=$(mktemp)
			createSources $TMPFILE
			sudo cp $TMPFILE $SCHROOTDIR/$D-$A/etc/apt/sources.list.d/extra.list
			rm $TMPFILE

			TRACE sudo schroot -c source:$D-$A -u root --directory=/ -- sh -c \
				"apt-get -qq update && \
				apt-get -qy upgrade && \
				apt-get -qy install apt-utils devscripts lsb-release apt-transport-https ca-certificates tzdata && \
				apt-get clean" < /dev/null
		else
			# Update chroot
			TRACE sudo schroot -c source:$D-$A -u root --directory=/ -- sh -c \
				"apt-get -qq update && apt-get -qy upgrade && apt-get clean" < /dev/null
		fi
	done
done
