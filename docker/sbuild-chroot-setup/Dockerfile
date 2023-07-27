# Docker image for creating and updating sbuild chroots
#
# Build docker image with: docker build -t sbuildchrootsetup docker/sbuild-chroot-setup
# then setup or update the chroots with:
# docker run --privileged --rm -v $(pwd):/work/ci-builder-scripts \
#   -v /var/lib/schroot/chroots:/var/lib/schroot/chroots \
#   -v /etc/schroot/chroot.d:/etc/schroot/chroot.d sbuildchrootsetup
ARG CODENAME=rolling

FROM ubuntu:${CODENAME}

VOLUME /work/ci-builder-scripts /work/target

RUN env LANG=C apt-get update -qq -o Acquire::Languages=none \
	&& env LANG=C DEBIAN_FRONTEND=noninteractive apt-get install -yqq \
	--no-install-recommends -o Dpkg::Options::=--force-unsafe-io \
	distro-info wget ca-certificates sudo ubuntu-dev-tools sbuild \
	schroot sendmail \
	&& apt-get clean && rm -rf /var/lib/apt/lists/*

CMD /work/ci-builder-scripts/bash/setup.sh
