/*
 * Copyright (c) 2018 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for Jenkins pre-merge builds of ci-builder-scripts repo
 */

freeStyleJob('Gerrit-CiBuilderScripts-preMerge') {
	description '''<p>Pre-merge builds of ci-builder-scripts.<p>
<p>The job is created by the DSL plugin from <i>CiBuilderScriptsJobs.groovy</i> script.</p>'''

	label 'packager'

	scm {
		git {
			remote {
				url('git://gerrit.lsdev.sil.org/ci-builder-scripts')
				refspec('$GERRIT_REFSPEC')
			}
			branch '$GERRIT_BRANCH'
			extensions {
				submoduleOptions {
					recursive(true)
				}
				cloneOptions {
					shallow(true)
					timeout(30)
					noTags(false)
				}
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
				draftPublished()
				patchsetCreated()
			}
			project('ci-builder-scripts', "ant:**")
		}
	}

	wrappers {
		timestamps()
		colorizeOutput()
		timeout {
			noActivity(1200)
			abortBuild()
			writeDescription("Build timed out after {0} minutes")
		}
	}

	steps {
		gradle {
			useWrapper(true)
			tasks('clean')
			tasks('test')
		}
		shell("""#!/bin/bash
if ! dpkg -l | grep -q shunit2; then
	sudo apt update
	sudo apt install shunit2
fi

cd bash/tests
for f in *.tests.sh; do
	./\$f
done
""")
	}

	publishers {
		publishHtml {
			report('build/reports/tests/test') {
				reportName('Test results')
			}
		}
		archiveJunit('build/test-results/**/*.xml')
	}
}
