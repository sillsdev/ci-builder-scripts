/*
 * DSL script for Jenkins Bloom GitHub jobs
 */
import utilities.common
import utilities.Bloom

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

for (branchName in ['master', 'Version3.6']) {

	freeStyleJob("GitHub-Bloom-Wrapper-$branchName-debug") {

		description """
<p>Wrapper job for GitHub pull requests of $branchName branch. This job kicks off
several other builds when a new pull request gets created or an existing one updated,
collects the results and reports them back to GitHub.</p>
<p>The job is created by the DSL plugin from <i>BloomGitHubJobs.groovy</i> script.</p>
"""

		// Bloom team decided that they don't want pre-merge builds
		disabled(true)

		parameters {
			stringParam("sha1", "",
				"What pull request to build, e.g. origin/pr/9/head")
		}

		label 'linux'

		properties {
			priority(100)
		}

		scm {
			git {
				remote {
					github("BloomBooks/BloomDesktop")
					refspec('+refs/pull/*:refs/remotes/origin/pr/*')
				}
				branch('${sha1}')
			}
		}

		triggers {
			githubPullRequest {
				admin('ermshiperete')
				useGitHubHooks(true)
				userWhitelist('StephenMcConnel hatton phillip-hopper davidmoore1 gmartin7 JohnThomson')
				orgWhitelist('BloomBooks')
				allowMembersOfWhitelistedOrgsAsAdmin(true)
				displayBuildErrorsOnDownstreamBuilds(true)
				cron('H/5 * * * *')
				whiteListTargetBranches([ branchName ])
			}
		}

		def branchNameForParameterizedTrigger=branchName.replaceAll('([^A-Za-z0-9])', '_').replaceAll('([_]+)', '_')

		steps {
			shell('echo -n ${BUILD_TAG} > ${WORKSPACE}/magic.txt')

			common.addTriggerDownstreamBuildStep(delegate,
				"GitHub-Bloom-Linux-any-$branchName-debug,GitHub-Bloom-Win32-$branchName-debug,GitHub-Bloom-Linux-any-$branchName--JSTests")

			common.addTriggerDownstreamBuildStep(delegate,
				"GitHub-Bloom-Linux-any-$branchName-debug-Tests, GitHub-Bloom-Win32-$branchName-debug-Tests",
				["ARTIFACTS_TAG":
				"jenkins-GitHub-Bloom-Win32-$branchName-debug-\${TRIGGERED_BUILD_NUMBERS_GitHub_Bloom_Win32_PR_debug}",
				"UPSTREAM_BUILD_TAG": "\${BUILD_TAG}"])

			copyArtifacts("GitHub-Bloom-Linux-any-$branchName-debug-Tests") {
				includePatterns 'output/Debug/BloomTests.dll.results.xml'
				targetDirectory "GitHub-Bloom-Linux-any-$branchName-debug-Tests-results/"
				flatten true
				optional true
				fingerprintArtifacts true
				buildSelector {
					buildNumber("\${TRIGGERED_BUILD_NUMBER_GitHub_Bloom_Linux_any_${branchNameForParameterizedTrigger}_debug_Tests}")
				}
			}

			copyArtifacts("GitHub-Bloom-Win32-$branchName-debug-Tests") {
				includePatterns 'output/Debug/BloomTests.dll.results.xml'
				targetDirectory "GitHub-Bloom-Win32-$branchName-debug-Tests-results/"
				flatten true
				optional true
				fingerprintArtifacts true
				buildSelector {
					buildNumber("\${TRIGGERED_BUILD_NUMBER_GitHub_Bloom_Win32_${branchNameForParameterizedTrigger}_debug_Tests}")
				}
			}

			copyArtifacts("GitHub-Bloom-Linux-any-$branchName--JSTests") {
				includePatterns 'src/BloomBrowserUI/test-results.xml'
				targetDirectory "GitHub-Bloom-Linux-any-$branchName--JSTests-results/"
				flatten true
				optional true
				fingerprintArtifacts true
				buildSelector {
					buildNumber("\${TRIGGERED_BUILD_NUMBER_GitHub_Bloom_Linux_any_${branchNameForParameterizedTrigger}_JSTests}")
				}
			}

		}

		publishers {
			fingerprint('magic.txt')
			archiveJunit("GitHub-Bloom-Linux-any-$branchName--JSTests-results/test-results.xml")
			configure common.NUnitPublisher('**/BloomTests.dll.results.xml')
		}

		common.buildPublishers(delegate, 365, 100)
	}

	// *********************************************************************************************
	freeStyleJob("GitHub-Bloom-Linux-any-$branchName-debug") {
		Bloom.defaultGitHubPRBuildJob(delegate,
			"Pre-merge builds of GitHub pull requests of $branchName branch")

		label 'ubuntu && supported'

		steps {
			// Install certificates
			common.addInstallPackagesBuildStep(delegate)

			// Get dependencies
			common.addGetDependenciesBuildStep(delegate)

			// Build
			common.addXbuildBuildStep(delegate, 'BloomLinux.sln')
		}
	}

	// *********************************************************************************************
	freeStyleJob("GitHub-Bloom-Linux-any-$branchName-debug-Tests") {
		Bloom.defaultGitHubPRBuildJob(delegate,
			"Run unit tests for pull request of $branchName branch")

		parameters {
			stringParam("UPSTREAM_BUILD_TAG", "",
				"The upstream build tag.")
		}

		label 'linux'

		customWorkspace "/home/jenkins/workspace/GitHub-Bloom-Linux-any-$branchName-debug"

		wrappers {
			common.addXvfbBuildWrapper(delegate)
			runOnSameNodeAs("GitHub-Bloom-Linux-any-$branchName-debug", true)
		}

		steps {
			// Run unit tests
			common.addRunUnitTestsLinuxBuildStep(delegate, 'BloomTests.dll')

			// this is needed so that upstream aggregation of unit tests works
			common.addMagicAggregationFile(delegate)
		}

		publishers {
			fingerprint('magic.txt')
			archiveArtifacts('output/Debug/BloomTests.dll.results.xml')
			configure common.NUnitPublisher('output/Debug/BloomTests.dll.results.xml')
		}
	}

	// *********************************************************************************************
	freeStyleJob("GitHub-Bloom-Win32-$branchName-debug") {
		Bloom.defaultGitHubPRBuildJob(delegate,
			"Pre-merge builds of GitHub pull requests of $branchName branch")

		label 'windows'

		steps {
			// Get dependencies
			common.addGetDependenciesWindowsBuildStep(delegate)

			common.addMsBuildStep(delegate, 'Bloom.sln')
		}
	}

	// *********************************************************************************************
	freeStyleJob("GitHub-Bloom-Win32-$branchName-debug-Tests") {
		Bloom.defaultGitHubPRBuildJob(delegate,
			"Run unit tests for pull requests of $branchName branch.")

		parameters {
			stringParam("ARTIFACTS_TAG", "", "The artifact tag")
			stringParam("UPSTREAM_BUILD_TAG", "", "The upstream build tag.")
		}

		label 'windows'

		wrappers {
			runOnSameNodeAs("GitHub-Bloom-Win32-$branchName-debug", true)
		}

		steps {
			// Run unit tests
			common.addRunUnitTestsWindowsBuildStep(delegate, 'BloomTests.dll')

			// this is needed so that upstream aggregation of unit tests works
			common.addMagicAggregationFileWindows(delegate)
		}

		publishers {
			fingerprint('magic.txt')
			archiveArtifacts('output/Debug/BloomTests.dll.results.xml')
			configure common.NUnitPublisher('output/Debug/BloomTests.dll.results.xml')
		}
	}

	// *********************************************************************************************
	freeStyleJob("GitHub-Bloom-Linux-any-$branchName--JSTests") {
		Bloom.defaultGitHubPRBuildJob(delegate,
			"Run JS unit tests for pull requests of $branchName branch")

		parameters {
			stringParam("UPSTREAM_BUILD_TAG", "",
				"The upstream build tag.")
		}

		label 'jstests'

		wrappers {
			common.addXvfbBuildWrapper(delegate)
		}

		steps {
			// Get dependencies
			common.addGetDependenciesBuildStep(delegate)

			// Install nodejs dependencies
			Bloom.addInstallKarmaBuildStep(delegate)

			// run unit tests
			Bloom.addRunJsTestsBuildStep(delegate)

			// this is needed so that upstream aggregation of unit tests works
			common.addMagicAggregationFile(delegate)
		}

		publishers {
			fingerprint('magic.txt')
			archiveJunit('output/browser/TESTS-*.xml')
			archiveArtifacts('output/browser/TESTS-*.xml')
		}
	}
 } // end of for loop
