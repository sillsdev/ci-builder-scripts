/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * some Common definitions for LfMerge related jobs
 */
class LfMerge {
	static void generalLfMergeBuildJob(jobContext, spec, sha1, useTimeout = true, addLanguageForge = false,
		githubRepo = "sillsdev/LfMerge", whereToRun = 'lfmerge', isPr = false) {
		jobContext.with {
			properties {
				priority(100)
			}

			label whereToRun

			logRotator(365, 100)

			wrappers {
				timestamps()
				colorizeOutput()
				if (useTimeout) {
					timeout {
						likelyStuck()
						abortBuild()
						writeDescription("Build timed out after {0} minutes")
					}
				}
			}

			if (addLanguageForge) {
				multiscm {
					git {
						remote {
							github(githubRepo, "https")
							refspec(spec)
						}
						branch(sha1)
						extensions {
							cloneOptions {
								noTags(false)
							}
						}
					}
					git {
						remote {
							github("sillsdev/web-languageforge", "https")
							refspec(isPr ? '+refs/heads/master:refs/remotes/origin/master' : spec)
						}
						branch(isPr ? '*/master' : sha1)
						extensions {
							relativeTargetDirectory('data/php')
							ignoreNotifyCommit()
						}
					}
				}
			} else {
				scm {
					git {
						remote {
							github(githubRepo, "https")
							refspec(spec)
						}
						branch(sha1)
						extensions {
							cloneOptions {
								noTags(false)
							}
						}
					}
				}
			}
		}
	}

	static void commonLfMergeBuildJob(jobContext, spec, sha1, useTimeout = true, addLanguageForge = false, isPr = false) {
		generalLfMergeBuildJob(jobContext, spec, sha1, useTimeout, addLanguageForge, "sillsdev/LfMerge", 'lfmerge', isPr)
		jobContext.with {
			steps {
				throttleConcurrentBuilds {
					categories(['lfmerge-build-one-per-node'])
					maxPerNode(1)
				}

				// Install dependencies
				downstreamParameterized {
					trigger('LfMerge_InstallDependencies-Linux-any-master-release') {
						block {
							buildStepFailure('FAILURE')
							failure('FAILURE')
							unstable('UNSTABLE')
						}

						parameters {
							predefinedProps([branch: sha1, refspec: spec])
						}
					}
				}

				// Compile mercurial
				shell('''#!/bin/bash -e
BUILD=Release . environ
echo "Compiling Mercurial"
mkdir -p tmp_hg
cd tmp_hg
[ -d hg ] || hg clone http://selenic.com/hg
cd hg
hg checkout 3.0.1
make local
cp -r mercurial ../../Mercurial/''')

				// Install composer and initialize LF php code
				shell('''#!/bin/bash -e
cd data/php/src
if [ -f mongo-1.4.1.installed ]; then
	echo "Removing PECL mongo extension"
	sudo pecl uninstall mongo-1.4.1
	sudo sh -c 'sed 's/extension=mongo.so//' -i /etc/php5/cli/php.ini'
	sudo rm /etc/php5/cli/conf.d/20-mongo.ini || true
	sudo rm /etc/php5/conf.d/20-mongo.ini || true
	rm mongo-1.4.1.installed
fi
if [ ! -f mongodb.installed ]; then
	echo "Installing PECL mongodb extension"
	DEBIAN_FRONTEND=noninteractive
	sudo apt-get -y install libpcre3-dev
	sudo pecl install mongodb || true
	if [ ! -f /etc/php/7.0/mods-available/mongodb.ini ]; then
		sudo mkdir -p /etc/php/7.0/mods-available
		sudo sh -c 'echo "extension=mongodb.so" >> /etc/php/7.0/mods-available/mongodb.ini'
	fi
	if [ ! -f /etc/php/7.0/cli/conf.d/20-mongodb.ini ]; then
		sudo mkdir -p /etc/php/7.0/cli/conf.d
		sudo ln -s /etc/php/7.0/mods-available/mongodb.ini /etc/php/7.0/cli/conf.d/20-mongodb.ini
	fi
	touch mongodb.installed
fi

COMPOSERJSON=$(git log --format=%H -1 composer.json)
COMPOSERJSON_PREV=$(cat composer.json.sha 2>/dev/null || true)

NODEVERSION=8
DISTRO="$(lsb_release -s -c)"

[ -f /etc/apt/sources.list.d/nodesource.list ] && OLDVERSION=$(cat /etc/apt/sources.list.d/nodesource.list | head -1 | sed 's/.*node_\([0-9]*\).*/\1/')

if [ ! -f /usr/bin/npm ] || [[ $OLDVERSION < $NODEVERSION ]]; then
	curl --silent https://deb.nodesource.com/gpgkey/nodesource.gpg.key | sudo apt-key add -
	echo "deb https://deb.nodesource.com/node_${NODEVERSION}.x $DISTRO main" | sudo tee /etc/apt/sources.list.d/nodesource.list
	echo "deb-src https://deb.nodesource.com/node_${NODEVERSION}.x $DISTRO main" | sudo tee -a /etc/apt/sources.list.d/nodesource.list
	sudo apt-get update
	sudo DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs
fi

if [ "$COMPOSERJSON" != "$COMPOSERJSON_PREV" ]; then
	git clean -dxf
	echo "Installing composer"
	php -r "readfile('https://getcomposer.org/installer');" > composer-setup.php
	php composer-setup.php
	php -r "unlink('composer-setup.php');"
	sudo mkdir -p /usr/local/bin
	sudo mv composer.phar /usr/local/bin/composer
	sudo apt update
	sudo DEBIAN_FRONTEND=noninteractive apt-get install -y php7.0-cli php7.0-curl php7.0-dev php7.0-gd php7.0-intl php7.0-mbstring php-pear php-xdebug
	sudo npm install --global gulp-cli jscs
	echo "Running refreshDeps.sh"
	cd ..
	./refreshDeps.sh
	echo $COMPOSERJSON > src/composer.json.sha
	# git clean got rid of this, so create it again
	touch src/mongodb.installed
fi
''')
				// Download dependencies
				// We use mono 4 for that because it fails with mono 3 due to some async bugs
				shell('''#!/bin/bash -e
echo "Downloading dependencies"
export MONO_PREFIX=/opt/mono4-sil
. environ
xbuild /t:DownloadDependencies build/LfMerge.proj
''')

				// Compile and run tests
				shell('''#!/bin/bash -e
echo "Compiling LfMerge and running unit tests"
BUILD=Release . environ
mozroots --import --sync
yes | certmgr -ssl https://go.microsoft.com
yes | certmgr -ssl https://nugetgallery.blob.core.windows.net
yes | certmgr -ssl https://nuget.org
xbuild /t:Test /v:detailed /property:Configuration=Release build/LfMerge.proj
result=$?

# Jenkins has problems using jgit to remove LinkedFiles directory with
# non-ASCII characters in filenames, so we delete these here
rm -rf data/testlangproj
rm -rf data/testlangproj-modified
exit $result
''')

			}

			Common.buildPublishers(delegate, 365, 100)

			publishers {
				Common.addNUnitPublisher(delegate, '**/TestResults.xml')
			}
		}
	}

}
