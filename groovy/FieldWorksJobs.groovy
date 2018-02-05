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

	scm {
		git {
			remote {
				url("git://gerrit.lsdev.sil.org/FieldWorks.git")
				refspec('$GERRIT_REFSPEC')
			}
			branch('$GERRIT_BRANCH')
			browser {
				gitWeb("https://github.com/sillsdev/FieldWorks/")
			}
			extensions {
				perBuildTag()
				cleanAfterCheckout()
				choosingStrategy {
					gerritTrigger()
				}
				cloneOptions {
					noTags(false)
				}
			}
			configure { git ->
				git / 'extensions' << 'hudson.plugins.git.extensions.impl.AuthorInChangelog'()
			}
		}
	}

	triggers {
		gerrit {
			project('FieldWorks', '$GERRIT_BRANCH')
			events {
				refUpdated()
			}
		}
	}

	wrappers {
		timestamps()
		colorizeOutput()
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

