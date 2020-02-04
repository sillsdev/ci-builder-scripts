/*
 * DSL script for package builder Jenkins jobs
 */

def distros = "trusty xenial bionic focal"

pipelineJob('SBuildChroots_Update-Linux-all') {
	description '''
<p>Maintenance job that updates all chroot instances for sbuild.</p>
<p>The job is created by the DSL plugin from <i>SBuildChrootJobs.groovy</i> script.</p>
'''

	logRotator(365, 100, 10, 10)

	parameters {
		stringParam("Distributions", distros,
			"The distributions to update")
	}

	triggers {
		// Run every Sunday
		// Times are UTC
		cron("H H * * 0")
	}

	definition {
		cps {
			script('''@Library('lsdev-pipeline-library') _
				runOnAllNodes(label: 'packager',
					command: '$HOME/ci-builder-scripts/bash/update --no-package --dists "$Distributions"')'''
			)
		}
	}
}

pipelineJob('SBuildChroots_Cleanup-Linux-all') {
	description '''
<p>Maintenance job that cleans out previously built binary packages and cancelled builds left on
disk that are at least two days old.</p>
<p>The job is created by the DSL plugin from <i>SBuildChrootJobs.groovy</i> script.</p>
'''

	logRotator(365, 100, 10, 10)

	triggers {
		// Run once every day
		// Times are UTC
		cron("H H * * *")
	}

	definition {
		cps {
			script('''@Library('lsdev-pipeline-library') _
				runOnAllNodes(label: 'packager',
					command: '$HOME/ci-builder-scripts/bash/clean-old-builds --no-package')'''
			)
		}
	}
}

pipelineJob('SBuildChroots_Setup-Linux-all') {
	description '''
<p>Maintenance job that creates chroot instances for sbuild. To be triggered manually.</p>
<p>The job is created by the DSL plugin from <i>SBuildChrootJobs.groovy</i> script.</p>
'''

	logRotator(365, 100, 10, 10)

	parameters {
		stringParam("Distributions", distros,
			"The distributions to create")
	}

	definition {
		cps {
			script('''@Library('lsdev-pipeline-library') _
				runOnAllNodes(label: 'packager',
					command: \'\'\'cd $HOME/ci-builder-scripts/bash
. ./common.sh
init --no-package "$@"

for distribution in $Distributions; do
	for arch in $ARCHES_TO_PACKAGE; do
		DISTRIBUTIONS="$distribution" ARCHES="$arch" $HOME/ci-builder-scripts/bash/setup.sh
	done
done\'\'\')
'''
			)
		}
	}
}
