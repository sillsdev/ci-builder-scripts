# Packaging Locally

The packaging scripts are intended to be run on a CI build server. However,
it's also possible to run them locally, e.g. to test the package build process.
This document describes how to locally build a package using the scripts in
the `bash` subdirectory.

## Preparation

Clone the project and set your working directory to the clone project. In
this documentation we assume you put your projects under `~/dev`.

```bash
git clone https://github.com/username/project ~/dev/project
cd ~/dev/project
```

## Creating source package

Use `make-source` to create a source package:

```bash
git clean -dxf && git reset --hard
~/dev/ci-builder-scripts/bash/make-source --build-in-place
```

**NOTE:** creating the source package modifies some files in the repo. Therefore
it's important to reset the repo to a clean state before running the
`make-source` script again.

This will create the source package in the parent directory of the package repo
(`~/dev` in the example above).

## Creating binary packages

Use `build-package` to build the binary packages for the different distros and
architectures.

```bash
~/dev/ci-builder-scripts/bash/build-package --dists "bionic focal"
	--arches "amd64 i386" --debkeyid $MYKEYID --build-in-place --no-upload
```

The resulting binary packages will be copied to `~/dev/results`.
