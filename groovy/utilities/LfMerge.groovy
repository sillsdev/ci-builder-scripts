/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */
/*
 * some common definitions for LfMerge related jobs
 */
package utilities
import utilities.Helper
import utilities.common

class LfMerge {
	static void generalLfMergeBuildJob(jobContext, spec, sha1, useTimeout = true, addLanguageForge = false, githubRepo = "sillsdev/LfMerge", whereToRun = 'lfmerge') {
		jobContext.with {
			properties {
				priority(100)
			}

			label whereToRun

			logRotator(365, 100)

			wrappers {
				timestamps()
				if (useTimeout) {
					timeout {
						likelyStuck()
						abortBuild()
						writeDescription("Build timed out after {0} minutes")
					}
				}
			}

			multiscm {
				git {
					remote {
						github(githubRepo, "https")
						refspec(spec)
					}
					branch(sha1)
				}
				if (addLanguageForge) {
					git {
						remote {
							github("sillsdev/web-languageforge", "https")
							refspec('+refs/heads/master:refs/remotes/origin/master')
						}
						branch('*/master')
						extensions {
							relativeTargetDirectory('data/php')
						}
					}
				}
			}
		}
	}

	static void commonLfMergeBuildJob(jobContext, spec, sha1, useTimeout = true, addLanguageForge = false) {
		generalLfMergeBuildJob(jobContext, spec, sha1, useTimeout, addLanguageForge)
		jobContext.with {
			steps {
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
				shell('''#!/bin/bash
set -e
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
				shell('''#!/bin/bash
set -e
cd data/php/src
if [ ! -f vendor/autoload.php ]; then
	echo "Installing composer and PHP dependencies"
	php -r "readfile('https://getcomposer.org/installer');" > composer-setup.php
	php -r "if (hash_file('SHA384', 'composer-setup.php') === 'a52be7b8724e47499b039d53415953cc3d5b459b9d9c0308301f867921c19efc623b81dfef8fc2be194a5cf56945d223') { echo 'Installer verified'; } else { echo 'Installer corrupt'; unlink('composer-setup.php'); } echo PHP_EOL;"
	php composer-setup.php
	php -r "unlink('composer-setup.php');"
	php composer.phar install
fi
if [ ! -f mongo-1.4.1.installed ]; then
	echo "Installing PECL mongo extension"
	sudo pecl install mongo-1.4.1
	touch mongo-1.4.1.installed
fi
if ! grep -q mongo.so /etc/php5/cli/php.ini; then
	echo "Setting mongo.so in php.ini"
	sudo sh -c 'echo "extension=mongo.so" >> /etc/php5/cli/php.ini'
fi
''')
				// Compile and run tests
				shell('''#!/bin/bash
set -e
echo "Compiling LfMerge and running unit tests"
BUILD=Release . environ
mozroots --import --sync
yes | certmgr -ssl https://go.microsoft.com
yes | certmgr -ssl https://nugetgallery.blob.core.windows.net
yes | certmgr -ssl https://nuget.org
xbuild /t:Test /property:Configuration=Release build/LfMerge.proj
exit $?''')

			}

			common.buildPublishers(delegate, 365, 100)

			publishers {
				configure common.NUnitPublisher('**/TestResults.xml')
			}
		}
	}

	static void addGitHubParamAndTrigger(jobContext, branch, os = 'linux') {
		jobContext.with {
			parameters {
				stringParam("sha1", "refs/heads/master",
					"What pull request to build, e.g. origin/pr/9/merge")
			}

			triggers {
				githubPullRequest {
					admin('ermshiperete')
					useGitHubHooks(true)
					orgWhitelist('sillsdev')
					cron('H/5 * * * *')
					allowMembersOfWhitelistedOrgsAsAdmin()
					displayBuildErrorsOnDownstreamBuilds(true)
					whiteListTargetBranches([ branch ])
					extensions {
						commitStatus {
							context("continuous-integration/jenkins-$os")
						}
					}
				}
			}
		}
	}

}
