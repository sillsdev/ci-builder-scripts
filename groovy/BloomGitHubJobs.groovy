/*
 * DSL script for Jenkins Bloom GitHub jobs
 */
import utilities.common

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
			userWhitelist('StephenMcConnel hatton phillip-hopper davidmoore1');
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

		downstreamParameterized {
			trigger('GitHub-Bloom-Linux-any-PR-debug,GitHub-Bloom-Win32-PR-debug',
				'ALWAYS', false,
				["buildStepFailure": "FAILURE", "failure": "FAILURE", "unstable": "UNSTABLE"]) {
				currentBuild()
			}
		}
		downstreamParameterized {
			trigger('GitHub-Bloom-Linux-any-PR-debug-Tests, Bloom-Win32-default-debug-Tests',
				'ALWAYS', false,
				["buildStepFailure": "FAILURE", "failure": "FAILURE", "unstable": "UNSTABLE"]) {
				currentBuild();
				predefinedProps('''ARTIFACTS_TAG="jenkins-GitHub-Bloom-Win32-PR-debug-${TRIGGERED_BUILD_NUMBERS_GitHub_Bloom_Win32_PR_debug}"
				UPSTREAM_BUILD_TAG=${BUILD_TAG}''')
			}
		}

		copyArtifacts('GitHub-Bloom-Linux-any-PR-debug-Tests', 'output/Debug/BloomTests.dll.results.xml',
			'GitHub-Bloom-Linux-any-PR-debug-Tests-results/', true, true) {
			latestSuccessful(false)
		}

		copyArtifacts('Bloom-Win32-default-debug-Tests', 'output/Debug/BloomTests.dll.results.xml',
			'Bloom-Win32-default-debug-Tests-results/', true, true) {
			latestSuccessful(false)
		}
	}

	publishers {
		fingerprint('magic.txt')
		configure { project ->
			project / 'publishers' << 'hudson.plugins.nunit.NUnitPublisher' {
				testResultsPattern('**/BloomTests.dll.results.xml')
			}
		}
	}

	common.buildPublishers(delegate, 365, 100);
}

class Bloom {
	static void defaultGitHubPRBuildJob(jobContext, jobName, descriptionVal) {
		jobContext.with {
			name jobName

			description '<p>' + descriptionVal + ''' This job gets triggered by GitHub-Bloom-Wrapper-debug.<p>
<p>The job is created by the DSL plugin from <i>BloomGitHubJobs.groovy</i> script.</p>''';

			parameters {
				stringParam("sha1", "",
					"What pull request to build, e.g. origin/pr/9/head");
			}

			priority(100);
			logRotator(365, 100);

			scm {
				git {
					remote {
						github("BloomBooks/BloomDesktop", "git");
						refspec('+refs/pull/*:refs/remotes/origin/pr/*')
					}
					branch('${sha1}')
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

			common.buildPublishers(delegate, 365, 100);
		}
	}
}

// *********************************************************************************************
job {
	Bloom.defaultGitHubPRBuildJob(delegate, 'GitHub-Bloom-Linux-any-PR-debug',
		'Pre-merge builds of GitHub pull requests.');

	label 'ubuntu && supported';

	steps {
		// Install certificates
		shell('''cd build
mozroots --import --sync
./install-deps''');

		// Get dependencies
		// REVIEW: do we need to set these environment variables? The node should already set them!
		shell('''cd build
export http_proxy=http://proxy.wycliffe.ca:4128
export HTTP_PROXY=http://proxy.wycliffe.ca:4128
./getDependencies-Linux.sh''');

		// Build
		shell('''. ./environ
xbuild Bloom\\ VS2010.sln''');
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

	configure { project ->
		project / 'buildWrappers' << 'org.jenkinsci.plugins.xvfb.XvfbBuildWrapper' {
			installationName 'default'
			screen '1024x768x24'
			displayNameOffset 1
		}
		project / 'buildWrappers' << 'com.datalex.jenkins.plugins.nodestalker.wrapper.NodeStalkerBuildWrapper' {
			job 'GitHub-Bloom-Linux-any-PR-debug'
			shareWorkspace true
			firstTimeFlag true
		}
	}

	// REVIEW: in the manual created job we also injected environment variables:
	// NO_PROXY=localhost
	// HTTP_PROXY=http://proxy.wycliffe.ca:4128
	// http_proxy=http://proxy.wycliffe.ca:4128
	// However, this should not be necessary since the node should set those.

	steps {
		// Run unit tests
		shell('''. ./environ
dbus-launch --exit-with-session
cd output/Debug
mono --debug ../../packages/NUnit.Runners.Net4.2.6.3/tools/nunit-console.exe -apartment=STA -nothread \
-labels -xml=BloomTests.dll.results.xml BloomTests.dll || true
exit 0
		''');

		shell('''# this is needed so that upstream aggregation of unit tests works
echo -n ${UPSTREAM_BUILD_TAG} > ${WORKSPACE}/magic.txt
		''');
	}

	publishers {
		fingerprint('magic.txt')
		archiveArtifacts('output/Debug/BloomTests.dll.results.xml')
		configure { project ->
			project / 'publishers' << 'hudson.plugins.nunit.NUnitPublisher' {
				testResultsPattern('output/Debug/BloomTests.dll.results.xml')
			}
		}
	}
}

// *********************************************************************************************
job {
	Bloom.defaultGitHubPRBuildJob(delegate, 'GitHub-Bloom-Win32-PR-debug',
		'Pre-merge builds of GitHub pull requests.');

	label 'windows';

	wrappers {
		environmentVariables {
			env('TEMP', 'C:\\cygwin\\home\\jenkins2\\tmp');
			env('TMP', 'C:\\cygwin\\home\\jenkins2\\tmp');
		}
	}

	steps {
		batchFile('''CD build
SET TEMP=%HOME%\\tmp
IF NOT EXIST %TEMP% MKDIR %TEMP%
echo which mkdir > %TEMP%\\%BUILD_TAG%.txt
echo ./getDependencies-windows.sh >> %TEMP%\\%BUILD_TAG%.txt
"c:\\Program Files (x86)\\Git\\bin\\bash.exe" --login -i < %TEMP%\\%BUILD_TAG%.txt
''');

	}

	configure { project ->
		project / 'builders' << 'hudson.plugins.msbuild.MsBuildBuilder' {
			msBuildName '.NET 4.0'
			msBuildFile 'Bloom VS2010.sln'
			cmdLineArgs ''
			buildVariablesAsProperties false
			continueOnBuildFailure false
			unstableIfWarnings false
		}

		project / 'builders' << 'org.jenkinsci.plugins.artifactdeployer.ArtifactDeployerBuilder' {
			entry {
				includes 'output/**/*, packages/NUnit.Runners.*/**/*, DistFiles/**/*, src/BloomBrowserUI/**/*, Mercurial/**/*, MercurialExtensions/**/*, lib/**/*'
				remote '$HOME/archive/$BUILD_TAG'
				deleteRemoteArtifacts false
			}
		}
	}

}

