/*
 * DSL script for package builder Jenkins jobs
 */

def distros = "bionic focal jammy lunar mantic"

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

	properties {
		pipelineTriggers {
			triggers {
				cron {
					// Run every Sunday
					// Times are UTC
					spec("H H * * 0")
				}
			}
		}
	}

	definition {
		cps {
			script('''@Library('lsdev-pipeline-library') _
				runOnAllNodes(label: 'dockerpackager',
					command: 'docker run --privileged --rm -v $HOME/ci-builder-scripts:/work/ci-builder-scripts -v /var/lib/schroot/chroots:/var/lib/schroot/chroots -v /etc/schroot/chroot.d:/etc/schroot/chroot.d sbuildchrootsetup /work/ci-builder-scripts/bash/update --no-package --dists "$Distributions"')'''
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

	properties {
		pipelineTriggers {
			triggers {
				cron {
					// Run once every day
					// Times are UTC
					spec("H H * * *")
				}
			}
		}
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
				runOnAllNodes(label: 'dockerpackager',
					command: \'\'\'
cd $HOME/ci-builder-scripts/bash
. ./common.sh
general_init

echo "Building Docker image..."
docker build -t sbuildchrootsetup $HOME/ci-builder-scripts/docker/sbuild-chroot-setup

for distribution in $Distributions; do
	for arch in $ARCHES_TO_PACKAGE; do
		echo "Building chroot for $distribution/$arch"
		docker run --privileged --rm -v $HOME/ci-builder-scripts:/work/ci-builder-scripts \
			-v /var/lib/schroot/chroots:/var/lib/schroot/chroots \
			-v /etc/schroot/chroot.d:/etc/schroot/chroot.d sbuildchrootsetup \
			/work/ci-builder-scripts/bash/setup.sh --dists "$distribution" --arches "$arch"
	done
done\'\'\')
'''
			)
		}
	}
}
