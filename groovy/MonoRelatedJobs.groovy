/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for Jenkins Mono jobs
 */
//#include utilities/Common.groovy

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

for (type in ['', 'Gerrit']) {
	for (repo in ['mono', 'gtk-sharp', 'libgdiplus', 'mono-basic']) {
		for (branchName in ['develop', 'release/mono-sil-3.4']) {
			def isGerritBuild = (type == 'Gerrit')

			freeStyleJob("${isGerritBuild ? type + '-' : ''}${repo.capitalize().replace('-', '')}-Linux-any-${branchName.replace('/', '_').replace('-', '_')}-debug") {

				description """<p>${isGerritBuild ? 'Gerrit build' : 'Build'} of <i>${branchName}</i> branch of custom ${repo} on Linux.<p>
<p>The job is created by the DSL plugin from <i>MonoRelatedJobs.groovy</i> script.</p>"""

				properties {
					priority(100)
				}

				label 'linux'

				logRotator(365, 100)

				concurrentBuild()

				if (!isGerritBuild) {
					parameters {
						stringParam('GERRIT_REFSPEC', "+refs/heads/${branchName}:refs/remotes/origin/${branchName}",
							"The refspec")
						stringParam('GERRIT_BRANCH', "${branchName}",
							"What branch to build")
					}
				}

				scm {
					git {
						remote {
							url("git://gerrit.lsdev.sil.org/${repo}.git")
							refspec('$GERRIT_REFSPEC')
						}
						branch('$GERRIT_BRANCH')
						browser {
							gitWeb("https://github.com/sillsdev/${repo}/")
						}
						extensions {
							perBuildTag()
							cleanAfterCheckout()
							choosingStrategy {
								gerritTrigger()
							}
						}
						configure { git ->
							git / 'extensions' << 'hudson.plugins.git.extensions.impl.AuthorInChangelog'()
						}
					}
				}

				triggers {
					gerrit {
						events {
							if (isGerritBuild) {
								patchsetCreated()
								draftPublished()
							} else {
								refUpdated()
							}
						}
						project(repo, branchName)
					}
				}

				wrappers {
					timestamps()
					colorizeOutput()
					timeout {
						noActivity(180)
						abortBuild()
						writeDescription("Build timed out after {0} minutes")
					}
				}

				steps {
					if (branchName == 'develop')
						MONO_PREFIX="/opt/mono4-sil"
					else
						MONO_PREFIX="/opt/mono-sil"

					shell('''#!/bin/bash
set -e
export MONO_PREFIX=''' + MONO_PREFIX + '''
# use ccache
export PATH=/usr/lib/ccache:$WORKSPACE/bin:$MONO_PREFIX/bin:$PATH

[ -e configure ] || NOCONFIGURE=1 ./autogen.sh
./configure --prefix=$WORKSPACE
make
''')
				}

				Common.buildPublishers(delegate, 365, 100)
			}
		}
	}
}