/*
 * DSL script to create some support jobs
 */

pipelineJob('InstallFieldWorksDependencies-Linux') {
	description '''<p>Install dependencies required on Linux to build FieldWorks.</p>
<p>The job is created by the DSL plugin from <i>FieldWorksJobs.groovy</i> script.<p>'''

	parameters {
		stringParam('GERRIT_REFSPEC', "+refs/heads/develop:refs/remotes/origin/develop",
			"The refspec")
		stringParam('GERRIT_BRANCH', "develop",
			"What branch to build")
	}

	properties {
		githubProjectUrl("https://github.com/sillsdev/FieldWorks/")
		pipelineTriggers {
			triggers {
				gerrit {
					gerritProjects {
						gerritProject {
							compareType('PLAIN')
							pattern('FieldWorks')
							branches {
								branch {
									compareType('PLAIN')
									pattern('$GERRIT_BRANCH')
								}
							}
							disableStrictForbiddenFileVerification(false)
						}
					}
					triggerOnEvents {
						refUpdated()
					}
				}
			}
		}
	}

	definition {
		cps {
			script('''@Library('lsdev-pipeline-library') _
				runOnAllNodes(label: 'fwsupported',
					command: \'\'\'
if [ ! -d "Build/Agent" ]; then
	git init
	git config core.sparseCheckout true
	git remote add -t $GERRIT_BRANCH origin git://gerrit.lsdev.sil.org/FieldWorks.git
	echo "Build/Agent/*" > .git/info/sparse-checkout
fi
git fetch --depth=1 origin
git checkout $GERRIT_BRANCH
git reset --hard origin/$GERRIT_BRANCH

Build/Agent/install-deps
\'\'\')'''
			)
		}
	}
}

