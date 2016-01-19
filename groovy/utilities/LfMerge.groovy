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
	static void generalLfMergeBuildJob(jobContext, spec, sha1, useTimeout = true) {
		jobContext.with {
			priority(100)
			label 'lfmerge'

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

			scm {
				git {
					remote {
						github("sillsdev/LfMerge", "git")
						refspec(spec)
					}
					branch(sha1)
				}
			}
		}
	}

	static void commonLfMergeBuildJob(jobContext, spec, sha1, useTimeout = true) {
		generalLfMergeBuildJob(jobContext, spec, sha1, useTimeout)
		jobContext.with {
			steps {
				// Install dependencies
				downstreamParameterized {
					trigger('LfMerge_InstallDependencies-Linux-any-master-debug') {
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
echo "Compiling Mercurial"
mkdir -p tmp_hg
cd tmp_hg
[ -d hg ] || hg clone http://selenic.com/hg
cd hg
hg checkout 3.0.1
make local
cp -r mercurial ../../Mercurial/''')

				// Compile and run tests
				shell('''#!/bin/bash
set -e
echo "Compiling LfMerge and running unit tests"
. environ
xbuild /t:Test /property:Configuration=Release build/LfMerge.proj
exit $?''')

			}

			common.buildPublishers(delegate, 365, 100)

			publishers {
				configure common.NUnitPublisher('**/TestResults.xml')
			}
		}
	}
}
