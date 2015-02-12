/*
 * DSL script for Jenkins Bloom GitHub jobs
 */
import utilities.common;
import utilities.Bloom;

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

job {
	name 'GitHub-Bloom-Wrapper-debug';

	description '''
<p>Wrapper job for GitHub pull requests. This job kicks off several other builds when a new
pull request gets created or an existing one updated, collects the results and reports them
back to GitHub.</p>
<p>The job is created by the DSL plugin from <i>BloomGitHubJobs.groovy</i> script.</p>
''';

	parameters {
		stringParam("sha1", "",
			"What pull request to build, e.g. origin/pr/9/head");
	}

	priority(100);

	scm {
		git {
			remote {
				github("BloomBooks/BloomDesktop");
				refspec('+refs/pull/*:refs/remotes/origin/pr/*')
			}
			branch('${sha1}')
		}
	}

	triggers {
		pullRequest {
			admin('ermshiperete');
			useGitHubHooks(true);
			userWhitelist('StephenMcConnel hatton phillip-hopper davidmoore1 gmartin7 JohnThomson');
			orgWhitelist('BloomBooks');
			cron('H/5 * * * *');
		}
		configure { project ->
			project / 'triggers' << 'org.jenkinsci.plugins.ghprb.GhprbTrigger' {
				displayBuildErrorsOnDownstreamBuilds(true);
				allowMembersOfWhitelistedOrgsAsAdmin(true);
			}
		}
	}

	steps {
		shell('echo -n ${BUILD_TAG} > ${WORKSPACE}/magic.txt')

		common.addTriggerDownstreamBuildStep(delegate,
			'GitHub-Bloom-Linux-any-PR-debug,GitHub-Bloom-Win32-PR-debug,GitHub-Bloom-Linux-any-PR--JSTests')

		common.addTriggerDownstreamBuildStep(delegate,
			'GitHub-Bloom-Linux-any-PR-debug-Tests, Bloom-Win32-default-debug-Tests',
			'''ARTIFACTS_TAG="jenkins-GitHub-Bloom-Win32-PR-debug-${TRIGGERED_BUILD_NUMBERS_GitHub_Bloom_Win32_PR_debug}"
UPSTREAM_BUILD_TAG=${BUILD_TAG}''')

		copyArtifacts('GitHub-Bloom-Linux-any-PR-debug-Tests', 'output/Debug/BloomTests.dll.results.xml',
			'GitHub-Bloom-Linux-any-PR-debug-Tests-results/', true, true) {
			latestSuccessful(false)
		}

		copyArtifacts('Bloom-Win32-default-debug-Tests', 'output/Debug/BloomTests.dll.results.xml',
			'Bloom-Win32-default-debug-Tests-results/', true, true) {
			latestSuccessful(false)
		}

		copyArtifacts('GitHub-Bloom-Linux-any-PR--JSTests', 'src/BloomBrowserUI/test-results.xml',
			'GitHub-Bloom-Linux-any-PR--JSTests-results/', true, true) {
			latestSuccessful(false)
		}

	}

	publishers {
		fingerprint('magic.txt')
		archiveJunit('GitHub-Bloom-Linux-any-PR--JSTests-results/test-results.xml');
		configure common.NUnitPublisher('**/BloomTests.dll.results.xml')
	}

	common.buildPublishers(delegate, 365, 100);
}

// *********************************************************************************************
job {
	Bloom.defaultGitHubPRBuildJob(delegate, 'GitHub-Bloom-Linux-any-PR-debug',
		'Pre-merge builds of GitHub pull requests.');

	label 'ubuntu && supported';

	steps {
		// Install certificates
		common.addInstallPackagesBuildStep(delegate);

		// Get dependencies
		common.addGetDependenciesBuildStep(delegate);

		// Build
		common.addXbuildBuildStep(delegate, 'Bloom\\ VS2010.sln');
	}
}

// *********************************************************************************************
job {
	Bloom.defaultGitHubPRBuildJob(delegate, 'GitHub-Bloom-Linux-any-PR-debug-Tests',
		'Run unit tests for pull request.');

	parameters {
		stringParam("UPSTREAM_BUILD_TAG", "",
			"The upstream build tag.");
	}

	label 'linux';

	customWorkspace '/home/jenkins/workspace/GitHub-Bloom-Linux-any-PR-debug';

	configure common.XvfbBuildWrapper();
	configure common.RunOnSameNodeAs('GitHub-Bloom-Linux-any-PR-debug', true);

	steps {
		// Run unit tests
		common.addRunUnitTestsLinuxBuildStep(delegate, 'BloomTests.dll')

		// this is needed so that upstream aggregation of unit tests works
		common.addMagicAggregationFile(delegate);
	}

	publishers {
		fingerprint('magic.txt')
		archiveArtifacts('output/Debug/BloomTests.dll.results.xml')
		configure common.NUnitPublisher('output/Debug/BloomTests.dll.results.xml')
	}
}

// *********************************************************************************************
job {
	Bloom.defaultGitHubPRBuildJob(delegate, 'GitHub-Bloom-Win32-PR-debug',
		'Pre-merge builds of GitHub pull requests.');

	label 'windows';

	steps {
		// Get dependencies
		common.addGetDependenciesWindowsBuildStep(delegate)
	}

	configure common.MsBuildBuilder('Bloom VS2010.sln')
	configure common.ArtifactDeployerPublisher('output/**/*, packages/NUnit.Runners.*/**/*, ' +
		'DistFiles/**/*, src/BloomBrowserUI/**/*, Mercurial/**/*, MercurialExtensions/**/*, lib/**/*',
		'$HOME/archive/$BUILD_TAG')
}

// *********************************************************************************************
job {
	Bloom.defaultGitHubPRBuildJob(delegate, 'GitHub-Bloom-Linux-any-PR--JSTests',
		'Run JS unit tests for pull request.');

	parameters {
		stringParam("UPSTREAM_BUILD_TAG", "",
			"The upstream build tag.");
	}

	label 'linux && !wheezy';

	steps {
		// Install nodejs dependencies
		common.addInstallKarmaBuildStep(delegate);

		// Get dependencies
		common.addGetDependenciesBuildStep(delegate);

		// run unit tests
		common.addRunJsTestsBuildStep(delegate, 'src/BloomBrowserUI');

		// this is needed so that upstream aggregation of unit tests works
		common.addMagicAggregationFile(delegate);
	}

	publishers {
		fingerprint('magic.txt')
		archiveJunit('src/BloomBrowserUI/test-results.xml');
		archiveArtifacts('src/BloomBrowserUI/test-results.xml')
	}
}
