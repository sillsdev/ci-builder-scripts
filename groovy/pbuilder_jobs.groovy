/*
 * DSL script for package builder Jenkins jobs
 */

def distros = "trusty xenial bionic"

freeStyleJob('PBuilder_Update-Linux-all-master-debug') {
	description '''
<p>Maintenance job that updates all chroot instances for pbuilder</p>
<p>The job is created by the DSL plugin from <i>pbuilder-jobs.groovy</i> script.</p>
'''

	label 'packager'

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

	wrappers {
		timestamps()
		colorizeOutput()
	}

	steps {
		shell('''
$HOME/ci-builder-scripts/bash/update --no-package --dists "$Distributions"
''')
	}
}

freeStyleJob('PBuilder_Cleanup-Linux-all-master-debug') {
	description '''
<p>Maintenance job that cleans out previously built binary packages and cancelled builds left on disk
that are at least two days old.</p>
<p>The job is created by the DSL plugin from <i>pbuilder-jobs.groovy</i> script.</p>
'''

	label 'packager'

	logRotator(365, 100, 10, 10)

	triggers {
		// Run once every day
		// Times are UTC
		cron("H H * * *")
	}

	wrappers {
		timestamps()
		colorizeOutput()
	}

	steps {
		shell('''
$HOME/ci-builder-scripts/bash/clean-old-builds --no-package
''')
	}
}

freeStyleJob('PBuilder_Setup-Linux-all-master-debug') {
	description '''
<p>Maintenance job that creates chroot instances for pbuilder. To be triggered manually.</p>
<p>The job is created by the DSL plugin from <i>pbuilder-jobs.groovy</i> script.</p>
'''

	label 'packager'

	logRotator(365, 100, 10, 10)

	parameters {
		stringParam("Distributions", distros,
			"The distributions to create")
	}

	wrappers {
		timestamps()
		colorizeOutput()
	}

	steps {
		shell('''
cd $HOME/ci-builder-scripts/bash
. ./common.sh
init "$@"

for distribution in $Distributions; do
	for arch in $ARCHES_TO_PACKAGE; do
		PBUILDERDIR="$pbuilder_path" DISTRIBUTIONS="$distribution" ARCHES="$arch" \
			$HOME/FwSupportTools/packaging/pbuilder/setup.sh
	done
done
''')
	}
}

