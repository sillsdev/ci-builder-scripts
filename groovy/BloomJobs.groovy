/*
 * DSL script for Jenkins Bloom jobs
 */
import utilities.common

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

job {
	name 'Bloom-Linux-any-master--JSTests';

	description '''
<p>This job runs JS unit tests for Bloom.</p>
<p>The job is created by the DSL plugin from <i>BloomJobs.groovy</i> script.</p>
''';

	label 'linux && !wheezy';

	scm {
		git {
			remote {
				github("BloomBooks/BloomDesktop", "git");
				refspec('+refs/pull/*:refs/remotes/origin/pr/*')
			}
			branch('master')
		}
	}

	wrappers {
		timestamps()
		timeout {
			noActivity 180
		}
	}

	// Job DSL currently doesn't support to abort the build in the case of a timeout.
	// Therefore we have to use this clumsy way to add it.
	configure { project ->
		project / 'buildWrappers' / 'hudson.plugins.build__timeout.BuildTimeoutWrapper' / 'operationList' {
			'hudson.plugins.build__timeout.operations.AbortOperation'()
		}
	}

	steps {
		// Install nodejs dependencies
		common.addInstallKarmaBuildStep(delegate);

		// Get dependencies
		common.addGetDependenciesBuildStep(delegate);

		// run unit tests
		common.addRunJsTestsBuildStep(delegate, 'src/BloomBrowserUI');
	}

	publishers {
		archiveJunit('src/BloomBrowserUI/test-results.xml');
	}

	common.buildPublishers(delegate, 365, 100);
}
