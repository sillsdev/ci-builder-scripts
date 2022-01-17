# Build docker image with: docker build -t pkgbuild docker/
# then build the source package in the host directory with the source with:
# docker run --rm -v $(pwd):/pkgbuild/source -v $(pwd)/..:/pkgbuild/orig pkgbuild
ARG CODENAME=impish

FROM ubuntu:${CODENAME}

VOLUME /pkgbuild/orig /pkgbuild/source

RUN env LANG=C apt-get update -qq -o Acquire::Languages=none \
	&& env LANG=C DEBIAN_FRONTEND=noninteractive apt-get install -yqq \
		--no-install-recommends -o Dpkg::Options::=--force-unsafe-io \
		build-essential devscripts equivs \
	&& apt-get clean && rm -rf /var/lib/apt/lists/*

COPY build.sh /bin/

WORKDIR /pkgbuild/source

CMD /bin/build.sh
