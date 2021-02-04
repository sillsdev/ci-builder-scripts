/*
 * DSL script for package builder Jenkins jobs
 */

def distros = "xenial bionic focal groovy hirsute"

pipelineJob('PBuilder_Setup-Linux-all') {
	description '''
<p>Maintenance job that creates chroot instances for pbuilder. To be triggered manually.</p>
<p>The job is created by the DSL plugin from <i>pbuilder-jobs.groovy</i> script.</p>
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
init "$@"

for distribution in $Distributions; do
	for arch in $ARCHES_TO_PACKAGE; do
		PBUILDERDIR="$pbuilder_path" DISTRIBUTIONS="$distribution" ARCHES="$arch" \
			$HOME/FwSupportTools/packaging/pbuilder/setup.sh
	done
done

PBUILDERDIR="$pbuilder_path" DISTRIBUTIONS="$distributions" ARCHES="$ARCHES_TO_PACKAGE" $HOME/FwSupportTools/packaging/pbuilder/setup.sh --update
\'\'\')
'''
			)
		}
	}
}
