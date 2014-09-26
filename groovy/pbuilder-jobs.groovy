/*
 * DSL script for package builder Jenkins jobs
 */

job {
    name 'PBuilder_Update-Linux-all-master-debug';

    description '''
<p>Maintenance job that updates all chroot instances for pbuilder</p>
<p>The job is created by the DSL plugin from <i>pbuilder-jobs.groovy</i> script.</p>
''';

	label 'packager';

	logRotator(365, 100, 10, 10);

	parameters {
		stringParam("Distributions", "precise saucy trusty wheezy jessie",
			"The distributions to update");
	}

	wrappers {
		timestamps();
	}

	steps {
		shell('''
$HOME/ci-builder-scripts/bash/update --dists "$Distributions"
''');
	}

	publishers {
		archiveArtifacts("results/*");

		configure { project ->
			project / publishers << 'hudson.plugins.jira.JiraIssueUpdater' {
			}
		}
	}

}

