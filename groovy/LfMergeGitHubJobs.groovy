/*
 * Copyright (c) 2016 SIL International
 * This software is licensed under the MIT license (http://opensource.org/licenses/MIT)
 */

/*
 * DSL script for Jenkins LfMerge jobs
 */
//#include utilities/Common.groovy
//#include utilities/LfMerge.groovy

// *********************************************************************************************
freeStyleJob('GitHub-LfMerge-Linux-any-master-release') {
	// Temporarily turned off LanguageForge dependency for PR builds until we get PHP 7 installed on the build agents. 2017-04-24 RM
	LfMerge.commonLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', true, false, true)

	description '''<p>Pre-merge Linux builds of master branch. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	Common.addGitHubParamAndTrigger(delegate, 'master')
}

// *********************************************************************************************
freeStyleJob('GitHub-Chorus-Linux-any-lfmerge-release') {

	description '''<p>Pre-merge Linux builds of lfmerge branch of Chorus. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	Common.addGitHubParamAndTrigger(delegate, 'lfmerge')
	LfMerge.generalLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', false, false, "sillsdev/chorus", 'linux&&supported')

	wrappers {
		Common.addXvfbBuildWrapper(delegate)
	}


	steps {
		shell('''#!/bin/bash
. environ
unset LD_PRELOAD

build/buildupdate.mono.sh

xbuild /t:Test /property:BUILD_NUMBER=0.0.$BUILD_NUMBER.0 /property:Configuration=ReleaseMono /property:excludedCategories=SkipOnBuildServer build/Chorus.proj''')
	}

	publishers {
		configure Common.NUnitPublisher('**/TestResults.xml')
	}
}

// *********************************************************************************************
freeStyleJob('GitHub-Chorus-Win32-lfmerge-release') {

	description '''<p>Pre-merge Windows builds of lfmerge branch of Chorus. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	Common.addGitHubParamAndTrigger(delegate, 'lfmerge', 'windows')
	LfMerge.generalLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', false, false, "sillsdev/chorus", 'windows&&timeInSync')

	steps {
		Common.addGetDependenciesWindowsBuildStep(delegate, 'build/buildupdate.win.sh')

		Common.addMsBuildStep(delegate, 'build\\Chorus.proj', '/t:Test /property:BUILD_NUMBER=0.0.%BUILD_NUMBER%.0 /property:Configuration=Release')
	}

	publishers {
		configure Common.NUnitPublisher('**/TestResults.xml')
	}
}

// *********************************************************************************************
freeStyleJob('GitHub-FlexBridge-Linux-any-lfmerge-release') {

	description '''<p>Pre-merge Linux builds of lfmerge branch of FLExBridge. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	Common.addGitHubParamAndTrigger(delegate, 'lfmerge')
	LfMerge.generalLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', false, false, "sillsdev/flexbridge", 'linux&&supported')

	wrappers {
		Common.addXvfbBuildWrapper(delegate)
	}

	steps {
		shell('''#!/bin/bash
BUILD=ReleaseMono
. environ

./download_dependencies_linux.sh

xbuild /t:Test /property:BUILD_NUMBER=0.0.$BUILD_NUMBER.0 /property:BUILD_VCS_NUMBER=$GIT_COMMIT /property:Configuration=$BUILD build/FLExBridge.proj''')
	}

	publishers {
		configure Common.NUnitPublisher('**/TestResults.xml')
	}
}

// *********************************************************************************************
freeStyleJob('GitHub-FlexBridge-Win32-lfmerge-release') {

	description '''<p>Pre-merge Windows builds of lfmerge branch of FLExBridge. Triggered by creating a PR on GitHub.<p>
<p>The job is created by the DSL plugin from <i>LfMergeGitHubJobs.groovy</i> script.</p>'''

	Common.addGitHubParamAndTrigger(delegate, 'lfmerge', 'windows')
	LfMerge.generalLfMergeBuildJob(delegate, '+refs/pull/*:refs/remotes/origin/pr/*', '${sha1}', false, false, "sillsdev/flexbridge", 'windows&&timeInSync')

	steps {
		Common.addGetDependenciesWindowsBuildStep(delegate, './download_dependencies_windows.sh')

		Common.addMsBuildStep(delegate, 'build\\FLExBridge.proj', '/t:Test /property:BUILD_NUMBER=0.0.%BUILD_NUMBER%.0 /property:BUILD_VCS_NUMBER=%GIT_COMMIT% /property:Configuration=Release')
	}

	publishers {
		configure Common.NUnitPublisher('**/TestResults.xml')
	}
}
