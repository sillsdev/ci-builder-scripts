/*
 * DSL script for Jenkins Bloom jobs
 */
import utilities.common
import utilities.Bloom

/*
 * Definition of jobs
 *
 * NOTE: if you want to rename jobs, rename them in Jenkins first, then change the name here
 * and commit and push the changes.
 */

freeStyleJob('Bloom-Wrapper-Trigger-debug') {
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
		common.addTriggerDownstreamBuildStep(delegate,
			'Bloom-Win32-master-debug,Bloom-Linux-any-master-debug')

		common.addTriggerDownstreamBuildStep(delegate,
			'Bloom-Linux-any-master-debug-Tests, Bloom-Win32-master-debug-Tests,Bloom-Linux-any-master--JSTests')

	}

	common.buildPublishers(delegate, 365, 100)
}

// *********************************************************************************************
freeStyleJob('Bloom-Linux-any-master-debug') {
	previousNames 'Bloom-Linux-any-default-debug'

	Bloom.defaultBuildJob(delegate, 'Linux builds of master branch')

	// geckofx 45 doesn't work on Precise
	label 'ubuntu && supported && !ubuntu12.04'

	steps {
		// Install certificates
		common.addInstallPackagesBuildStep(delegate)

		// Get dependencies
		common.addGetDependenciesBuildStep(delegate)

		// Build
		common.addXbuildBuildStep(delegate, 'build/Bloom.proj', '/t:Build /p:BUILD_NUMBER=0.0.${BUILD_ID}.${GIT_COMMIT}')
	}
}

// *********************************************************************************************
freeStyleJob('Bloom-Win32-master-debug') {
	previousNames 'Bloom-Win32-default-debug'

	Bloom.defaultBuildJob(delegate, 'Windows builds of master branch')

	label 'windows'

	steps {
		// Get dependencies
		common.addGetDependenciesWindowsBuildStep(delegate)

		common.addMsBuildStep(delegate, 'build\\Bloom.proj', '/t:Build /p:BUILD_NUMBER=0.0.${BUILD_ID}.${GIT_COMMIT}')
	}
}


// *********************************************************************************************
freeStyleJob('Bloom-Linux-any-master-debug-Tests') {
	previousNames 'Bloom-Linux-any-default-debug-Tests'

	Bloom.defaultBuildJob(delegate, 'Run unit tests.')

	label 'linux'

	customWorkspace '/home/jenkins/workspace/Bloom-Linux-any-master-debug'

	wrappers {
		common.addXvfbBuildWrapper(delegate)
		runOnSameNodeAs('Bloom-Linux-any-master-debug', true)
	}

	steps {
		// Run unit tests
		common.addXbuildBuildStep(delegate, 'build/Bloom.proj', '/t:TestOnly /p:BUILD_NUMBER=0.0.${BUILD_ID}.${GIT_COMMIT}')
	}

	publishers {
		configure common.NUnitPublisher('output/Debug/BloomTests.dll.results.xml')
	}
}

// *********************************************************************************************
freeStyleJob('Bloom-Win32-master-debug-Tests') {
	previousNames 'Bloom-Win32-default-debug-Tests'

	Bloom.defaultBuildJob(delegate, 'Run Bloom unit tests.')

	parameters {
		stringParam("ARTIFACTS_TAG", "", "The artifact tag")
		stringParam("UPSTREAM_BUILD_TAG", "", "The upstream build tag.")
	}

	label 'windows'

	wrappers {
		runOnSameNodeAs('Bloom-Win32-master-debug', true)
	}

	steps {
		// Run unit tests
		common.addMsBuildStep(delegate, 'build\\Bloom.proj', '/t:TestOnly /p:BUILD_NUMBER=0.0.${BUILD_ID}.${GIT_COMMIT}')

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
		common.addXvfbBuildWrapper(delegate)
	}

	steps {
		// Get dependencies
		common.addGetDependenciesBuildStep(delegate)

		// Install nodejs dependencies
		Bloom.addInstallKarmaBuildStep(delegate)

		// run unit tests
		Bloom.addRunJsTestsBuildStep(delegate)
	}

	publishers {
		archiveJunit('output/browser/TESTS-*.xml')
	}

	common.buildPublishers(delegate, 365, 100)
}
