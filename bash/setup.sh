#!/bin/bash

# setup.sh
# Setup or update mirrors

set -e -o pipefail

PROGRAM=$(readlink -f "$0")
PROGRAM_NAME="$(basename "$0")"
PROGRAM_DIR="$(realpath "$(dirname "$0")")"

function helpScript() {
	cat << EOF

$PROGRAM_NAME --dists <distros> --arches <arches>

Setup chroot.

Arguments:
	--dists <distros> - the distributions to create a chroot for
	--arches <arches> - the architectures to create a chroot for

There are more optional parameters that can be set. See common.sh.

NOTE: This help is incomplete. Please add the description of more options
that are useful and apply to this script.
EOF
	exit 0
}

. "${PROGRAM_DIR}"/common.sh

# shellcheck disable=SC2034 # used in common.sh
no_package=true

init "$@"

function isUnknownDistro()
{
	! ubuntu-distro-info --series="$1" >/dev/null 2>&1 && ! debian-distro-info --series="$1" >/dev/null 2>&1
}

function isUbuntu()
{
	ubuntu-distro-info --series="$1" >/dev/null 2>&1
}

function isDebian()
{
	debian-distro-info --series="$1" >/dev/null 2>&1
}

function isSupported()
{
	if [[ "$(ubuntu-distro-info --supported) $(ubuntu-distro-info --supported-esm) $(debian-distro-info --supported)" =~ $1 ]]; then
		return 0
	fi
	return 1
}

function checkOrLinkDebootstrapScript()
{
	if [ ! -f /usr/share/debootstrap/scripts/"$1" ]; then
		if isUbuntu "$1"; then
			basedistro=gutsy
		else
			basedistro=sid
		fi
		sudo ln -s /usr/share/debootstrap/scripts/$basedistro /usr/share/debootstrap/scripts/"$1"
	fi
}

function addmirror()
{
	OTHERMIRROR="$OTHERMIRROR${OTHERMIRROR:+|}$1"
}

# Create a source.list template file that will be used in the chroot
function createSources()
{
	cat > "$1" <<EOF
$(echo "$OTHERMIRROR" | tr '|' '\n')
EOF
}

function enableBackports()
{
	cat > "$1" <<EOF
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
		touch "$HOME/.sbuildrc"
	elif [ ! -f "$HOME/.sbuildrc" ]; then
		touch "$HOME/.sbuildrc"
	fi

	if [ -n "$TOINSTALL" ]; then
		log "Installing prerequisites: $TOINSTALL"
		sudo apt-get update
		sudo apt-get -qy install "$TOINSTALL"
	fi

	GROUP=$(groups | grep sbuild || true)
	if [ -z "$GROUP" ]; then
		sudo adduser "$USER" sbuild > /dev/null || true
		local TEMPFILE
		local ARGS=()
		TEMPFILE=$(mktemp)
		while (( $# )); do
			ARGS+=("\"$1\"")
			shift
		done
		echo "${ARGS[@]}" > "$TEMPFILE"
		chmod +x "$TEMPFILE"

		log "Calling $(basename "${ARGS[0]}") recursively"
		sg sbuild "$TEMPFILE"
		rm "$TEMPFILE"
		exit
	fi

	# We have to install a current version of mk-sbuild because trying to build newer dists
	# on an older dist might have different requirements than the system provided version
	# of mk-sbuild provides (e.g. on xenial when trying to build a bionic chroot).
	if [ ! -f "$HOME/bin/mk-sbuild" ] || [ ! -f "$HOME/bin/mk-sbuild.v${MKSBUILD_VERSION}" ]; then
		log "Getting version ${MKSBUILD_VERSION} of mk-sbuild"
		mkdir -p "$HOME/bin"
		rm -f "$HOME"/bin/mk-sbuild*
		TRACE wget --output-document="$HOME/bin/mk-sbuild" https://git.launchpad.net/ubuntu-dev-tools/plain/mk-sbuild?h="${MKSBUILD_VERSION}"
		chmod +x "$HOME/bin/mk-sbuild"
		touch "$HOME/bin/mk-sbuild.v${MKSBUILD_VERSION}"
	fi

	# We also need a fairly recent version of debootstrap - the version provided in Xenial is
	# too old
	if [ ! -f "$HOME/bin/debootstrap.v${DEBOOTSTRAP_VERSION}" ]; then
		log "Installing version ${DEBOOTSTRAP_VERSION} of debootstrap"
		pushd /tmp
		TRACE wget "https://mirrors.kernel.org/ubuntu/pool/main/d/debootstrap/debootstrap_${DEBOOTSTRAP_VERSION}_all.deb"
		sudo dpkg -i "debootstrap_${DEBOOTSTRAP_VERSION}_all.deb"
		popd
		touch "$HOME/bin/debootstrap.v${DEBOOTSTRAP_VERSION}"
	fi

	installFileSbuildrc
}

function installFileSbuildrc()
{
	SBUILDRC_PATH="${HOME}/.sbuildrc"
	if diff "${SBUILDRC_PATH}" "${PROGRAM_DIR}"/sbuildrc; then
		return
	fi
	log "Installing .sbuildrc"
	# Temporarily backup any existing .sbuildrc, in case we are overwriting something good.
	cp -v "${SBUILDRC_PATH}" "$(mktemp /tmp/old-sbuildrc.XXXXXXXXXX)" || true
	cp -v "${PROGRAM_DIR}"/sbuildrc "${SBUILDRC_PATH}"
}

function copyInKeyrings()
{
	sudo mkdir -p "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRINGLLSO}" ] && sudo cp "${KEYRINGLLSO}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRINGPSO}" ] && sudo cp "${KEYRINGPSO}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRINGNODE}" ] && sudo cp "${KEYRINGNODE}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRINGMICROSOFT}" ] && sudo cp "${KEYRINGMICROSOFT}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRING_MONO}" ] && sudo cp "${KEYRING_MONO}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
	[ -f "${KEYRING_MICROSOFTPROD}" ] && sudo cp "${KEYRING_MICROSOFTPROD}" "${SCHROOTDIR}/${D}-${A}/etc/apt/trusted.gpg.d/"
}

function addExtraRepositories()
{
	TMPFILE="$(mktemp)"
	createSources "${TMPFILE}"
	sudo mkdir -p "${SCHROOTDIR}/${D}-${A}/etc/apt/sources.list.d"
	sudo cp "${TMPFILE}" "${SCHROOTDIR}/${D}-${A}/etc/apt/sources.list.d/extra.list"
	rm "${TMPFILE}"
}

function doesChrootExist()
{
	local dist=$1
	local arch=$2

	sudo schroot --list | grep -q "source:${dist}-${arch}" && \
		sudo schroot --list | grep -q "chroot:${dist}-${arch}"
}

function downloadAndExportKey()
{
	local keyfile=$1
	local fingerprint=$2
	local tmp_keyring

	if [ ! -f "${keyfile}" ]; then
		tmp_keyring="$(mktemp)"
		gpg --trust-model always --no-default-keyring --keyring "${tmp_keyring}" \
			--keyserver keyserver.ubuntu.com --recv-keys "${fingerprint}"
		gpg --no-default-keyring --keyring "${tmp_keyring}" --export > "${keyfile}"
		rm -f "${tmp_keyring}"
	fi
}

function updateDistroInfo()
{
	# see /usr/share/doc/distro-info-data/README.Debian
	wget --output-document=/tmp/debian.csv https://debian.pages.debian.net/distro-info-data/debian.csv
	wget --output-document=/tmp/ubuntu.csv https://debian.pages.debian.net/distro-info-data/ubuntu.csv
	sudo cp /tmp/debian.csv /usr/share/distro-info
	sudo cp /tmp/ubuntu.csv /usr/share/distro-info
}

WORKDIR="${WORKSPACE:-${PROGRAM_DIR}}"

cd "${WORKDIR}"

updateDistroInfo

checkAndInstallRequirements "$PROGRAM" "$@"

KEYRINGLLSO="$WORKDIR/llso-keyring-2013.gpg"
KEYRINGPSO="$WORKDIR/pso-keyring-2016.gpg"
KEYRINGNODE="$WORKDIR/nodesource-keyring.gpg"
KEYRINGMICROSOFT="$WORKDIR/microsoft.asc.gpg"
KEYRING_MONO="$WORKDIR/mono-project.asc.gpg"
KEYRING_MICROSOFTPROD="$WORKDIR/microsoft-prod.asc.gpg"

if [ ! -f "${KEYRINGPSO}" ]; then
	wget --output-document="${KEYRINGPSO}" https://packages.sil.org/keys/pso-keyring-2016.gpg
fi

if [ ! -f "${KEYRINGLLSO}" ]; then
	wget --output-document="${KEYRINGLLSO}" http://linux.lsdev.sil.org/keys/llso-keyring-2013.gpg
fi

if [ ! -f "${KEYRINGNODE}" ]; then
	wget -O- https://deb.nodesource.com/gpgkey/nodesource.gpg.key | gpg --dearmor -o "${KEYRINGNODE}"
fi

if [ ! -f "${KEYRINGMICROSOFT}" ]; then
	wget -O- https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor -o "${KEYRINGMICROSOFT}"
fi

downloadAndExportKey "${KEYRING_MONO}" "3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF"
downloadAndExportKey "${KEYRING_MICROSOFTPROD}" "BC528686B50D79E339D3721CEB3E94ADBE1229CF"

for D in ${dists_arg:-$(ubuntu-distro-info --supported) $(debian-distro-info --testing) $(debian-distro-info --stable) $(debian-distro-info --oldstable)}
do
	if isUnknownDistro "$D"; then
		echo "Unknown distro $D. Maybe you'll have to update /usr/share/distro-info/ubuntu.csv."
		continue
	fi

	for A in ${arches_arg:-$ARCHES_TO_PROCESS}
	do
		doesChrootExist "$D" "$A" && [ -z "$update" ] && echo "$D-$A already exists - skipping creation" && continue
		! doesChrootExist "$D" "$A" && [ -n "$update" ] && echo "$D-$A doesn't exist - skipping update" && continue

		# Starting with Ubuntu 21.10 (Impish) there is only 64-bit available
		if [ "$A" == "i386" ] && (( $(ubuntu-distro-info --series="$D" -r|cut -d'.' -f1) >= 21 )); then
			log "Skipping 32bit chroot for $D"
			continue
		fi

		log "Processing $D-$A"

		if ! doesChrootExist "$D" "$A"; then
			# Remove remnants of failed build
			[ -e "$SCHROOTDIR/$D-$A" ] && sudo rm -rf "$SCHROOTDIR/$D-$A"
		fi

		OTHERMIRROR=""

		checkOrLinkDebootstrapScript "$D"

		if isUbuntu "$D"; then
			DISTRO=ubuntu
			# LTSDIST=$(ubuntu-distro-info --lts)
			# packages.microsoft is a 64-bit only repo. 32-bit can be downloaded as a tar.
			if [ "$A" != "i386" ]; then
				if [ "$D" == "$(ubuntu-distro-info --devel)" ]; then
					MICROSOFT_APT="deb [arch=amd64] https://packages.microsoft.com/ubuntu/$(ubuntu-distro-info --series="${UBUNTU_LAST_RELEASE_MICROSOFT}" -r | cut -d' ' -f1)/prod ${UBUNTU_LAST_RELEASE_MICROSOFT} main"
				else
					MICROSOFT_APT="deb [arch=amd64] https://packages.microsoft.com/ubuntu/$(ubuntu-distro-info --series="${D}" -r | cut -d' ' -f1)/prod ${D} main"
				fi
				addmirror "${MICROSOFT_APT}"
			fi
			if (( $(ubuntu-distro-info --series="$D" -r|cut -d'.' -f1) >= 20 )); then
				MONO_APT="deb https://download.mono-project.com/repo/ubuntu stable-focal main"
			elif (( $(ubuntu-distro-info --series="$D" -r|cut -d'.' -f1) >= 18 )); then
				MONO_APT="deb https://download.mono-project.com/repo/ubuntu stable-bionic main"
			else
				MONO_APT="deb https://download.mono-project.com/repo/ubuntu stable-xenial main"
			fi

			if isSupported "$D"; then
				MIRROR="${UBUNTU_MIRROR:-http://archive.ubuntu.com/ubuntu/}"
			else
				MIRROR="${UBUNTU_OLDMIRROR:-http://old-releases.ubuntu.com/ubuntu/}"
			fi
			COMPONENTS="main universe multiverse"
			# KEYRINGMAIN="/usr/share/keyrings/ubuntu-archive-keyring.gpg"
			PROXY="$http_proxy"
			for S in backports ; do
				addmirror "deb $MIRROR $D-$S $COMPONENTS"
			done
			LLSO="http://linux.lsdev.sil.org/ubuntu/"
			PSO="https://packages.sil.org/ubuntu/"
			for S in "" "-proposed" "-updates" "-experimental"; do
				addmirror "deb $LLSO $D$S $COMPONENTS"
				addmirror "deb $PSO $D$S $COMPONENTS"
			done

			addmirror "${MONO_APT}"

			# Allow to install current nodejs packages
			if [ -n "$update" ]; then
				if [ "$D" == "bionic" ] || [ "$D" == "focal" ]; then
					# Starting with Groovy (Ubuntu 20.10) Node 12 is available as Debian packages
					addmirror "deb https://deb.nodesource.com/node_12.x $D main"
				fi
			fi
		elif isDebian "$D"; then
			DISTRO=debian
			if [ "$A" != "i386" ]; then
				# packages.microsoft is a 64-bit only repo. 32-bit can be downloaded as a tar.
				MICROSOFT_APT="deb [arch=amd64] https://packages.microsoft.com/repos/microsoft-debian-${D}-prod ${D} main"
				addmirror "${MICROSOFT_APT}"
			fi
			MONO_APT="deb https://download.mono-project.com/repo/debian vs-${D} main"

			MIRROR="${DEBIAN_MIRROR:-http://ftp.ca.debian.org/debian/}"
			COMPONENTS="main contrib non-free"
			# KEYRINGMAIN="/usr/share/keyrings/debian-archive-keyring.gpg"
			PROXY="$http_proxy"
			LLSO="http://linux.lsdev.sil.org/debian/"
			PSO="https://packages.sil.org/debian/"
			addmirror "deb $LLSO $D $COMPONENTS"
			addmirror "deb $PSO $D $COMPONENTS"
			addmirror "${MONO_APT}"
			# Allow to install current nodejs packages
			if [ -n "$update" ]; then
				if [ "$D" != "bullseye" ]; then
					# Starting with Bullseye Node 12 is available as Debian packages
					addmirror "deb https://deb.nodesource.com/node_12.x $D main"
				fi
			fi
		else
			stderr "Unknown distribution $D. Please update the script $0."
			exit 1
		fi

		if [ -z "$update" ]; then
			# Create a new chroot
			log "Create new chroot for $D-$A"

			# Build chroot - if that fails remove it
			if ! TRACE "$HOME/bin/mk-sbuild" --distro $DISTRO "$D" --arch="$A" \
					--debootstrap-include="perl,gnupg,debhelper,ca-certificates" \
					${PROXY:+--debootstrap-proxy=}"$PROXY" \
					"$OTHEROPTS" --type=directory; then
				sudo rm -rf "$SCHROOTDIR/$D-$A"
				continue
			fi

			copyInKeyrings
			addExtraRepositories

			TMPFILE=$(mktemp)
			enableBackports "$TMPFILE" "$D"
			sudo mkdir -p "$SCHROOTDIR/$D-$A/etc/apt/preferences.d"
			sudo cp "$TMPFILE" "$SCHROOTDIR/$D-$A/etc/apt/preferences.d/backports"
			rm "$TMPFILE"

			PKGLIST="apt-utils devscripts lsb-release apt-transport-https tzdata"

			log "Install packages in chroot for $D-$A"
			if [ "$(lsb_release --codename --short)" == "xenial" ]; then
				# Running on Xenial which has an older sbuild version that has a buggy sbuild-apt
				TRACE sudo schroot -c "source:$D-$A" -u root --directory=/ -- sh -c \
					"apt-get -qq update && \
					apt-get -qy upgrade && \
					apt-get -qy install $PKGLIST && \
					apt-get clean" < /dev/null
			else
				if [ "$D" == "xenial" ]; then
					# When building a Xenial chroot we have to install some additional
					# packages in the chroot
					TRACE sudo sbuild-apt "$D-$A" apt-get install apt-transport-https ca-certificates
				fi

				TRACE sudo sbuild-update --update --dist-upgrade --upgrade "$D-$A"
				# shellcheck disable=SC2086 # we want PKGLIST to be split in separate arguments!
				TRACE sudo sbuild-apt "$D-$A" apt-get install $PKGLIST
				TRACE sudo sbuild-update --clean --autoclean --autoremove "$D-$A"
			fi
		else
			# Update chroot
			log "Update chroot for $D-$A"
			copyInKeyrings
			addExtraRepositories

			TRACE sudo sbuild-apt "$D-$A" apt-get update
			TRACE sudo sbuild-update --update --dist-upgrade --upgrade --clean --autoclean --autoremove "$D-$A"
		fi
	done
done
