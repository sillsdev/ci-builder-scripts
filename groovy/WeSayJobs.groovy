/*
 * Copyright (c) 2017 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

import utilities.common

/*
 * DSL script for Jenkins WeSay jobs
 */

for (kind in ['Gerrit', 'normal']) {
	for (os in ['Linux', 'Windows']) {
		if (kind == 'Gerrit') {
			prefix = 'Gerrit-'
		} else {
			prefix = ''
		}

		freeStyleJob("${prefix}WeSay-$os-any-debug") {

			if (kind == 'Gerrit') {
				description """<p>Pre-merge $os build for WeSay. Triggered by pushing a change to Gerrit.<p>
<p>The job is created by the DSL plugin from <i>WeSayJobs.groovy</i> script.</p>"""
			} else {
				description """<p>$os build for WeSay.<p>
<p>The job is created by the DSL plugin from <i>WeSayJobs.groovy</i> script.</p>"""
			}

			parameters {
				stringParam('GERRIT_BRANCH', 'develop', 'The branch to build, e.g. develop')
				stringParam('GERRIT_REFSPEC', '', 'The refspec to build, e.g. refs/changes/69/3869/1')
			}

			properties {
				priority(100)
			}

			if (os == 'Linux') {
				label 'linux && supported'
			} else {
				label 'windows && timeInSync'
			}

			logRotator(365, 100)

			scm {
				git {
					remote {
						url('git://gerrit.lsdev.sil.org/wesay.git')
						refspec('$GERRIT_REFSPEC')
					}
					branch('$GERRIT_BRANCH')

					extensions {
						cleanAfterCheckout()
						choosingStrategy {
							gerritTrigger()
						}
					}
				}
			}

			triggers {
				gerrit {
					events {
						if (kind == 'Gerrit') {
							draftPublished()
							patchsetCreated()
						} else {
							refUpdated()
						}
					}
					project('wesay', 'ant:**')
				}
			}

			wrappers {
				timestamps()
				colorizeOutput()
				timeout {
					likelyStuck()
					abortBuild()
					writeDescription("Build timed out after {0} minutes")
				}
				if (os == 'Linux') {
					xvfb('default')
				}
			}

			steps {
				if (os == 'Linux') {
					shell('''#!/bin/bash
cd build
./agent/install-deps
./buildupdate.mono.sh
./TestBuild.sh''')
				} else {
					common.addGetDependenciesWindowsBuildStep(delegate, 'build/buildupdate.win.sh')
					common.addMsBuildStep(delegate, 'build\\build.win.proj', '/t:Test')
				}
			}
		}
	}
}