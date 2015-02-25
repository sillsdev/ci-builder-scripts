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

 for (branchName in ['master', 'Version3.0']) {

	job {
		name "GitHub-Bloom-Wrapper-$branchName-debug";

		description """
<p>Wrapper job for GitHub pull requests of $branchName branch. This job kicks off
several other builds when a new pull request gets created or an existing one updated,
collects the results and reports them back to GitHub.</p>
<p>The job is created by the DSL plugin from <i>BloomGitHubJobs.groovy</i> script.</p>
""";

		parameters {
			stringParam("sha1", "",
				"What pull request to build, e.g. origin/pr/9/head");
		}

		label 'linux';

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
		}

		// There's something weird going on: if we directly use branchName as the name for the
		// branch below, it'll insert the last value in the list (Version3.0). If instead we
		// define a variable and use that everything works as expected.
		def BRANCHNAME=branchName
		configure { project ->
			project / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' / 'displayBuildErrorsOnDownstreamBuilds'(true)
			project / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' / 'allowMembersOfWhitelistedOrgsAsAdmin'(true)
			project / 'triggers' / 'org.jenkinsci.plugins.ghprb.GhprbTrigger' / 'whiteListTargetBranches' {
				'org.jenkinsci.plugins.ghprb.GhprbBranch' { 'branch'(BRANCHNAME) }
			}
		}

		steps {
			shell('echo -n ${BUILD_TAG} > ${WORKSPACE}/magic.txt')

			common.addTriggerDownstreamBuildStep(delegate,
				"GitHub-Bloom-Linux-any-$branchName-debug,GitHub-Bloom-Win32-$branchName-debug,GitHub-Bloom-Linux-any-$branchName--JSTests")

			common.addTriggerDownstreamBuildStep(delegate,
				"GitHub-Bloom-Linux-any-$branchName-debug-Tests, GitHub-Bloom-Win32-$branchName-debug-Tests",
				"""ARTIFACTS_TAG=\"jenkins-GitHub-Bloom-Win32-$branchName-debug-\${TRIGGERED_BUILD_NUMBERS_GitHub_Bloom_Win32_PR_debug}"
UPSTREAM_BUILD_TAG=\${BUILD_TAG}""")

			copyArtifacts("GitHub-Bloom-Linux-any-$branchName-debug-Tests", 'output/Debug/BloomTests.dll.results.xml',
				"GitHub-Bloom-Linux-any-$branchName-debug-Tests-results/", true, true) {
				latestSuccessful(false)
			}

			copyArtifacts("GitHub-Bloom-Win32-$branchName-debug-Tests", 'output/Debug/BloomTests.dll.results.xml',
				"GitHub-Bloom-Win32-$branchName-debug-Tests-results/", true, true) {
				latestSuccessful(false)
			}

			copyArtifacts("GitHub-Bloom-Linux-any-$branchName--JSTests", 'src/BloomBrowserUI/test-results.xml',
				"GitHub-Bloom-Linux-any-$branchName--JSTests-results/", true, true) {
				latestSuccessful(false)
			}

		}

		publishers {
			fingerprint('magic.txt')
			archiveJunit("GitHub-Bloom-Linux-any-$branchName--JSTests-results/test-results.xml");
			configure common.NUnitPublisher('**/BloomTests.dll.results.xml')
		}

		common.buildPublishers(delegate, 365, 100);
	}

	// *********************************************************************************************
	job {
		Bloom.defaultGitHubPRBuildJob(delegate, "GitHub-Bloom-Linux-any-$branchName-debug",
			"Pre-merge builds of GitHub pull requests of $branchName branch");

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
		Bloom.defaultGitHubPRBuildJob(delegate, "GitHub-Bloom-Linux-any-$branchName-debug-Tests",
			"Run unit tests for pull request of $branchName branch");

		parameters {
			stringParam("UPSTREAM_BUILD_TAG", "",
				"The upstream build tag.");
		}

		label 'linux';

		customWorkspace "/home/jenkins/workspace/GitHub-Bloom-Linux-any-$branchName-debug";

		configure common.XvfbBuildWrapper();
		configure common.RunOnSameNodeAs("GitHub-Bloom-Linux-any-$branchName-debug", true);

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
		Bloom.defaultGitHubPRBuildJob(delegate, "GitHub-Bloom-Win32-$branchName-debug",
			"Pre-merge builds of GitHub pull requests of $branchName branch");

		label 'windows';

		steps {
			// Get dependencies
			common.addGetDependenciesWindowsBuildStep(delegate)
		}

		configure common.MsBuildBuilder('Bloom VS2010.sln')
	}

	// *********************************************************************************************
	job {
		Bloom.defaultGitHubPRBuildJob(delegate, "GitHub-Bloom-Win32-$branchName-debug-Tests",
			"Run unit tests for pull requests of $branchName branch.");

		parameters {
			stringParam("ARTIFACTS_TAG", "", "The artifact tag");
			stringParam("UPSTREAM_BUILD_TAG", "", "The upstream build tag.");
		}

		label 'windows';

		configure common.RunOnSameNodeAs("GitHub-Bloom-Win32-$branchName-debug", true);

		steps {
			// Run unit tests
			common.addRunUnitTestsWindowsBuildStep(delegate, 'BloomTests.dll')

			// this is needed so that upstream aggregation of unit tests works
			common.addMagicAggregationFileWindows(delegate);
		}

		publishers {
			fingerprint('magic.txt')
			archiveArtifacts('output/Debug/BloomTests.dll.results.xml')
			configure common.NUnitPublisher('output/Debug/BloomTests.dll.results.xml')
		}
	}

	// *********************************************************************************************
	job {
		Bloom.defaultGitHubPRBuildJob(delegate, "GitHub-Bloom-Linux-any-$branchName--JSTests",
			"Run JS unit tests for pull requests of $branchName branch");

		parameters {
			stringParam("UPSTREAM_BUILD_TAG", "",
				"The upstream build tag.");
		}

		label 'jstests';

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
 } // end of for loop
