/*
 * DSL script for Jenkins Bloom jobs
 */
//#include utilities/Common.groovy
//#include utilities/Bloom.groovy

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

freeStyleJob('Bloom-Wrapper-Trigger-release') {
	description '''
<p>Wrapper job for Bloom builds. This job kicks off several other builds after a new
change got merged and collects the results.</p>
<p>The job is created by the DSL plugin from <i>BloomJobs.groovy</i> script.</p>
'''

	properties {
		priority(100)
	}

	label 'linux'

	scm {
		git {
			remote {
				github("BloomBooks/BloomDesktop")
			}
			branch('*/master')
		}
	}

	triggers {
		githubPush()
	}

	steps {
		// Trigger downstream build
		Common.addTriggerDownstreamBuildStep(delegate,
			'Bloom-Win32-master-release,Bloom-Linux-any-master-release')

		Common.addTriggerDownstreamBuildStep(delegate,
			'Bloom-Linux-any-master-release-Tests, Bloom-Win32-master-release-Tests,Bloom-Linux-any-master--JSTests')

	}

	Common.buildPublishers(delegate, 365, 100)
}

// *********************************************************************************************
freeStyleJob('Bloom-Linux-any-master-release') {
	previousNames 'Bloom-Linux-any-default-release'

	Bloom.defaultBuildJob(delegate, 'Linux builds of master branch')

	// geckofx 45 doesn't work on Precise
	label 'ubuntu && supported && !ubuntu12.04'

	steps {
		// Install certificates
		Common.addInstallPackagesBuildStep(delegate)

		// Get dependencies
		Common.addGetDependenciesBuildStep(delegate)

		// Build
		Common.addXbuildBuildStep(delegate, 'build/Bloom.proj', '/t:Build /p:BUILD_NUMBER=0.0.${BUILD_ID}.${GIT_COMMIT}')
	}
}

// *********************************************************************************************
freeStyleJob('Bloom-Win32-master-release') {
	previousNames 'Bloom-Win32-default-release'

	Bloom.defaultBuildJob(delegate, 'Windows builds of master branch')

	label 'windows && nodejs'

	steps {
		// Get dependencies
		Common.addGetDependenciesWindowsBuildStep(delegate)

		batchFile('''cd src\\BloomBrowserUI
npm install
npm run build
''')

		Common.addMsBuildStep(delegate, 'build\\Bloom.proj', '/t:Build /p:BUILD_NUMBER=0.0.${BUILD_ID}.${GIT_COMMIT}', 'msbuild14')
	}
}


// *********************************************************************************************
freeStyleJob('Bloom-Linux-any-master-release-Tests') {
	previousNames 'Bloom-Linux-any-default-release-Tests'

	Bloom.defaultBuildJob(delegate, 'Run unit tests.')

	label 'linux'

	customWorkspace '/home/jenkins/workspace/Bloom-Linux-any-master-release'

	wrappers {
		Common.addXvfbBuildWrapper(delegate)
		runOnSameNodeAs('Bloom-Linux-any-master-release', true)
	}

	steps {
		// Run unit tests
		Common.addXbuildBuildStep(delegate, 'build/Bloom.proj', '/t:TestOnly /p:BUILD_NUMBER=0.0.${BUILD_ID}.${GIT_COMMIT}')
	}

	publishers {
		configure Common.NUnitPublisher('output/Release/TestResults.xml')
	}
}

// *********************************************************************************************
freeStyleJob('Bloom-Win32-master-release-Tests') {
	previousNames 'Bloom-Win32-default-release-Tests'

	Bloom.defaultBuildJob(delegate, 'Run Bloom unit tests.')

	parameters {
		stringParam("ARTIFACTS_TAG", "", "The artifact tag")
		stringParam("UPSTREAM_BUILD_TAG", "", "The upstream build tag.")
	}

	label 'windows && nodejs'

	wrappers {
		runOnSameNodeAs('Bloom-Win32-master-release', true)
	}

	steps {
		// Run unit tests
		Common.addMsBuildStep(delegate, 'build\\Bloom.proj', '/t:TestOnly /p:BUILD_NUMBER=0.0.${BUILD_ID}.${GIT_COMMIT}', '.NET 4.5')

		// this is needed so that upstream aggregation of unit tests works
		Common.addMagicAggregationFileWindows(delegate)
	}

	publishers {
		fingerprint('magic.txt')
		archiveArtifacts('output/Release/TestResults.xml')
		configure Common.NUnitPublisher('output/Release/TestResults.xml')
	}
}

// *********************************************************************************************
freeStyleJob('Bloom-Linux-any-master--JSTests') {
	description '''
<p>This job runs JS unit tests for Bloom.</p>
<p>The job is created by the DSL plugin from <i>BloomJobs.groovy</i> script.</p>
'''

	label 'jstests'

	scm {
		git {
			remote {
				github("BloomBooks/BloomDesktop", "git")
				refspec('+refs/pull/*:refs/remotes/origin/pr/*')
			}
			branch('master')
		}
	}

	wrappers {
		colorizeOutput()
		timestamps()
		timeout {
			noActivity 180
			abortBuild()
		}
		Common.addXvfbBuildWrapper(delegate)
	}

	steps {
		// Get dependencies
		Common.addGetDependenciesBuildStep(delegate)

		// Install nodejs dependencies
		Bloom.addInstallKarmaBuildStep(delegate)

		// run unit tests
		Bloom.addRunJsTestsBuildStep(delegate)
	}

	publishers {
		archiveJunit('output/browser/TESTS-*.xml')
		flowdock('608a6152ead8516caa955b81cda7c2cc') {
			aborted(true)
			failure(true)
			fixed(true)
			unstable(true)
			tags('jenkins')
		}
	}

	Common.buildPublishers(delegate, 365, 100)
}
